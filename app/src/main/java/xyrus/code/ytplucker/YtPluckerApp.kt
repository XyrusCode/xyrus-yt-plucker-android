package xyrus.code.ytplucker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import xyrus.code.ytplucker.data.AppPreferences
import xyrus.code.ytplucker.data.FeatureFlags
import xyrus.code.ytplucker.data.UpdateChecker
import xyrus.code.ytplucker.data.UpdateInfo

class YtPluckerApp : Application() {

    lateinit var featureFlags: FeatureFlags
        private set

    lateinit var preferences: AppPreferences
        private set

    lateinit var updateChecker: UpdateChecker
        private set

    private val _pendingUpdate = MutableStateFlow<UpdateInfo?>(null)
    val pendingUpdate: StateFlow<UpdateInfo?> = _pendingUpdate.asStateFlow()

    override fun onCreate() {
        super.onCreate()

        initSentry()
        createNotificationChannel()
        preferences = AppPreferences(this)

        updateChecker = UpdateChecker(this, BuildConfig.VERSION_NAME)
        // FeatureFlags guards Firebase internally, but flag setup must never take
        // down launch — fall back to a no-op instance with default flags.
        featureFlags = try {
            FeatureFlags(this)
        } catch (e: Exception) {
            Sentry.captureException(e)
            FeatureFlags.disabled()
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                YoutubeDL.init(this@YtPluckerApp)
                FFmpeg.init(this@YtPluckerApp)
                runCatching { YoutubeDL.updateYoutubeDL(this@YtPluckerApp) }
                    .onFailure { Log.w(TAG, "yt-dlp self-update skipped", it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize yt-dlp/ffmpeg", e)
                Sentry.captureException(e)
            }

            val update = runCatching {
                updateChecker.checkForUpdate()
            }.onFailure { Log.w(TAG, "Update check failed", it) }.getOrNull()
            if (update != null) {
                _pendingUpdate.value = update
            }
        }
    }

    private fun initSentry() {
        if (BuildConfig.SENTRY_DSN.isNotEmpty()) {
            SentryAndroid.init(this) {
                it.dsn = BuildConfig.SENTRY_DSN
                it.release = BuildConfig.APPLICATION_ID + "@" + BuildConfig.VERSION_NAME
                it.environment = if (BuildConfig.DEBUG) "debug" else "release"
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_channel_name),
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
