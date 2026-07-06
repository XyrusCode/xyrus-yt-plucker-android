package tech.acachi.ytplucker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YtPluckerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // youtubedl-android unpacks its Python + yt-dlp + ffmpeg payload on first init.
        // Do it off the main thread so app startup stays snappy.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@YtPluckerApp)
                FFmpeg.getInstance().init(this@YtPluckerApp)
                // The pinned wrapper (0.14.0) ships an old yt-dlp binary; refresh it so extraction
                // works against current sites. Best-effort — a failure here (e.g. offline) just
                // leaves the bundled binary in place.
                runCatching { YoutubeDL.getInstance().updateYoutubeDL(this@YtPluckerApp) }
                    .onFailure { Log.w(TAG, "yt-dlp self-update skipped", it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize yt-dlp/ffmpeg", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_channel_name),
                // LOW keeps the ongoing progress notification quiet (no sound/peek).
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.download_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "downloads"
        private const val TAG = "YtPluckerApp"
    }
}
