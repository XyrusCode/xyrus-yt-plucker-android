package xyrus.code.ytplucker.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyrus.code.ytplucker.domain.model.Quality
import xyrus.code.ytplucker.service.DownloadService

class BrowserViewModel(app: Application) : AndroidViewModel(app) {

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _isVideoPage = MutableStateFlow(false)
    val isVideoPage: StateFlow<Boolean> = _isVideoPage.asStateFlow()

    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl: StateFlow<String?> = _pendingUrl.asStateFlow()

    private val videoPatterns = listOf(
        Regex("""(?:m\.)?youtube\.\w+/watch\?v=[\w-]+"""),
        Regex("""youtu\.be/[\w-]+"""),
        Regex("""(?:x|twitter)\.\w+/\w+/status/\d+"""),
    )

    fun onUrlEntered(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val url = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else trimmed
        _pendingUrl.value = url
    }

    fun onPageNavigated(url: String) {
        _currentUrl.value = url
        _isVideoPage.value = videoPatterns.any { it.containsMatchIn(url) }
    }

    fun loadUrl(url: String) {
        _pendingUrl.value = url
    }

    fun onPendingUrlConsumed() {
        _pendingUrl.value = null
    }

    fun triggerDownload(url: String, quality: Quality) {
        val context = getApplication<Application>()
        val jobId = "job-${System.currentTimeMillis()}"
        val intent = DownloadService.startIntent(context, jobId, url, quality, destDir = null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
