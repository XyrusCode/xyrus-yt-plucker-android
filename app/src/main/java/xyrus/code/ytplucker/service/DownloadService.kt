package xyrus.code.ytplucker.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xyrus.code.ytplucker.R
import xyrus.code.ytplucker.YtPluckerApp
import xyrus.code.ytplucker.data.DownloadRepositoryImpl
import xyrus.code.ytplucker.data.MediaExporter
import xyrus.code.ytplucker.domain.model.DownloadProgress
import xyrus.code.ytplucker.domain.model.JobStatus
import xyrus.code.ytplucker.domain.model.Quality
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Foreground service that owns the lifecycle of active downloads. Declared with the
 * `dataSync` foreground-service type (see AndroidManifest) so the OS grants it sustained
 * network + CPU while the app is minimized or the screen is off. A partial WakeLock is held
 * only while at least one download is running, and released the instant the last one ends.
 *
 * The actual byte transfer is done by yt-dlp (via [DownloadManager]) writing straight to
 * disk — nothing is buffered in the heap here.
 */
class DownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val repository = DownloadRepositoryImpl

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID)
                if (jobId != null) cancelJob(jobId)
            }
            else -> {
                val jobId = intent?.getStringExtra(EXTRA_JOB_ID) ?: return dropIfIdle(startId)
                val url = intent.getStringExtra(EXTRA_URL) ?: return dropIfIdle(startId)
                val quality = Quality.fromId(intent.getStringExtra(EXTRA_QUALITY) ?: Quality.BEST.id)
                val destDir = intent.getStringExtra(EXTRA_DEST) ?: defaultDestDir()
                startDownload(jobId, url, quality, destDir)
            }
        }
        // Re-deliver the original intent if the process is killed mid-download.
        return START_REDELIVER_INTENT
    }

    private fun startDownload(jobId: String, url: String, quality: Quality, destDir: String) {
        // Promote to foreground immediately (Android requires startForeground within ~5s).
        val initial = DownloadProgress(
            jobId = jobId,
            title = shortTitleFrom(url),
            percent = 0f,
            speedBytesPerSec = -1f,
            etaSeconds = -1,
            status = JobStatus.RUNNING,
        )
        repository.publish(initial)
        startForegroundWithNotification(jobId, initial)
        acquireWakeLock()

        val job = serviceScope.launch {
            var last = initial
            // Download into a private working dir; publish the finished file to the public
            // gallery afterwards. Keeps partials out of the user's Movies/Music while in flight.
            val workDir = File(cacheDir, "ytwork/$jobId")
            try {
                workDir.mkdirs()
                val onProgress: (Float, Long, String) -> Unit = { percent, etaSeconds, line ->
                    val speed = parseSpeedBytesPerSec(line)
                    last = last.copy(
                        title = parseTitle(line) ?: last.title,
                        percent = percent.coerceIn(0f, 100f),
                        speedBytesPerSec = speed ?: last.speedBytesPerSec,
                        etaSeconds = etaSeconds,
                        status = JobStatus.RUNNING,
                    )
                    repository.publish(last)
                    updateNotification(jobId, last)
                }
                suspend fun runOnce() = repository.downloadManager.download(
                    url = url, quality = quality, destDir = workDir.absolutePath, jobId = jobId,
                    onProgress = onProgress,
                )
                try {
                    runOnce()
                } catch (e: Exception) {
                    // If extraction failed because the bundled yt-dlp has gone stale against a
                    // site change, pull the latest binary and retry once — self-healing.
                    if (isCancellation(e) || !isStaleEngineError(e.message)) throw e
                    notifyUpdating(jobId, last)
                    repository.downloadManager.updateEngine(applicationContext)
                    runOnce()
                }
                // Publish finished media into the public gallery (Movies/Pictures/Music by type).
                exportProduced(workDir)
                repository.publish(last.copy(percent = 100f, status = JobStatus.COMPLETED))
            } catch (e: Exception) {
                val cancelled = isCancellation(e)
                repository.publish(
                    last.copy(
                        status = if (cancelled) JobStatus.CANCELLED else JobStatus.FAILED,
                        error = if (cancelled) null else e.message,
                    ),
                )
            } finally {
                workDir.deleteRecursively()
                onJobFinished(jobId, last)
            }
        }
        jobs[jobId] = job
    }

    /** Copy the finished media file(s) from the working dir into the public gallery by type. */
    private fun exportProduced(workDir: File) {
        val media = workDir.listFiles()?.filter {
            it.isFile && it.length() > 0 &&
                !it.name.endsWith(".part", true) &&
                !it.name.endsWith(".ytdl", true) &&
                !it.name.endsWith(".tmp", true)
        }.orEmpty()
        media.forEach { f -> runCatching { MediaExporter.export(this, f) } }
    }

    private fun cancelJob(jobId: String) {
        repository.downloadManager.cancel(jobId)
        jobs[jobId]?.cancel()
    }

    private fun onJobFinished(jobId: String, last: DownloadProgress) {
        jobs.remove(jobId)
        NotificationManagerCompat.from(this).cancel(jobId.hashCode())
        if (jobs.isEmpty()) {
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun dropIfIdle(startId: Int): Int {
        if (jobs.isEmpty()) stopSelf(startId)
        return START_NOT_STICKY
    }

    // --- Notifications --------------------------------------------------------------------

    private fun startForegroundWithNotification(jobId: String, p: DownloadProgress) {
        val notification = buildNotification(jobId, p)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                jobId.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(jobId.hashCode(), notification)
        }
    }

    private fun updateNotification(jobId: String, p: DownloadProgress) {
        if (hasNotifPermission()) {
            NotificationManagerCompat.from(this).notify(jobId.hashCode(), buildNotification(jobId, p))
        }
    }

    /** Show an indeterminate "Updating downloader…" notification while yt-dlp self-updates. */
    private fun notifyUpdating(jobId: String, p: DownloadProgress) {
        repository.publish(p.copy(percent = 0f, speedBytesPerSec = -1f, etaSeconds = -1, status = JobStatus.RUNNING))
        if (!hasNotifPermission()) return
        val notif = NotificationCompat.Builder(this, YtPluckerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(p.title)
            .setContentText(getString(R.string.updating_engine))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(this).notify(jobId.hashCode(), notif)
    }

    private fun hasNotifPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.app.ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun isCancellation(e: Throwable): Boolean =
        e is kotlinx.coroutines.CancellationException ||
            e is InterruptedException ||
            e.message?.contains("cancel", ignoreCase = true) == true

    private fun buildNotification(jobId: String, p: DownloadProgress): Notification {
        val pct = p.percent.toInt()
        val speed = formatSpeed(p.speedBytesPerSec)
        val stopIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_JOB_ID, jobId)
        }
        val stopPending = android.app.PendingIntent.getService(
            this, jobId.hashCode(), stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, YtPluckerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(p.title)
            .setContentText("Downloading: $pct% • $speed")
            .setProgress(100, pct, pct == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.action_cancel), stopPending)
            .build()
    }

    // --- Wake lock ------------------------------------------------------------------------

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
            setReferenceCounted(false)
            acquire(WAKELOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun defaultDestDir(): String =
        (getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: filesDir).absolutePath

    companion object {
        const val ACTION_START = "xyrus.code.ytplucker.action.START"
        const val ACTION_STOP = "xyrus.code.ytplucker.action.STOP"
        const val EXTRA_JOB_ID = "jobId"
        const val EXTRA_URL = "url"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_DEST = "dest"

        private const val WAKELOCK_TAG = "ytplucker:download"
        private const val WAKELOCK_TIMEOUT_MS = 6L * 60 * 60 * 1000 // 6h safety cap

        /** Build the start intent used by the UI layer. */
        fun startIntent(
            context: Context,
            jobId: String,
            url: String,
            quality: Quality,
            destDir: String?,
        ): Intent = Intent(context, DownloadService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_JOB_ID, jobId)
            putExtra(EXTRA_URL, url)
            putExtra(EXTRA_QUALITY, quality.id)
            if (destDir != null) putExtra(EXTRA_DEST, destDir)
        }
    }
}
