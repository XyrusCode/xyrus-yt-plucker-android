package xyrus.code.ytplucker.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyrus.code.ytplucker.data.DownloadRepositoryImpl
import xyrus.code.ytplucker.domain.DownloadRepository
import xyrus.code.ytplucker.domain.model.DownloadProgress
import xyrus.code.ytplucker.domain.model.Quality
import xyrus.code.ytplucker.domain.model.VideoMeta
import xyrus.code.ytplucker.service.DownloadService

/** Immutable, @Stable-friendly UI state — one object drives the whole screen. */
data class UiState(
    val url: String = "",
    val quality: Quality = Quality.BEST,
    val meta: VideoMeta? = null,
    val analyzing: Boolean = false,
    val error: String? = null,
    val jobs: List<DownloadProgress> = emptyList(),
)

class DownloadViewModel(app: Application) : AndroidViewModel(app) {

    // Singleton bridge to the service. Kept as a property (not a constructor arg) so the
    // default AndroidViewModelFactory's (Application)-only constructor lookup succeeds.
    private val repository: DownloadRepository = DownloadRepositoryImpl

    // Local form/input state, merged with the repository's live progress into one UiState.
    private val form = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState> = combine(form, repository.progress) { f, progress ->
        f.copy(jobs = progress.values.sortedBy { it.jobId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun onUrlChange(value: String) = form.update { it.copy(url = value, error = null) }

    fun onQualityChange(quality: Quality) = form.update { it.copy(quality = quality) }

    fun onAnalyze() {
        val url = form.value.url.trim()
        if (url.isEmpty()) return
        form.update { it.copy(analyzing = true, error = null, meta = null) }
        viewModelScope.launch {
            runCatching { repository.probe(url) }
                .onSuccess { meta -> form.update { it.copy(analyzing = false, meta = meta) } }
                .onFailure { e -> form.update { it.copy(analyzing = false, error = e.message ?: "Analyze failed") } }
        }
    }

    /** Fire-and-forget: hand the work to the foreground service, which owns the lifecycle. */
    fun onDownload() {
        val state = form.value
        val url = state.url.trim()
        if (url.isEmpty()) {
            form.update { it.copy(error = "Enter a URL first") }
            return
        }
        val context = getApplication<Application>()
        val jobId = "job-${System.currentTimeMillis()}"
        val intent = DownloadService.startIntent(context, jobId, url, state.quality, destDir = null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun onCancel(jobId: String) {
        val context = getApplication<Application>()
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_STOP
            putExtra(DownloadService.EXTRA_JOB_ID, jobId)
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
