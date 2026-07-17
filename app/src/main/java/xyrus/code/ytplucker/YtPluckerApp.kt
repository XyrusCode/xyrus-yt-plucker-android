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
import xyrus.code.ytplucker.data.AppPreferences
import xyrus.code.ytplucker.data.FeatureFlags

class YtPluckerApp : Application() {

    lateinit var featureFlags: FeatureFlags
        private set

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()

        initSentry()
        createNotificationChannel()
        preferences = AppPreferences(this)
        featureFlags = FeatureFlags(this)

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
