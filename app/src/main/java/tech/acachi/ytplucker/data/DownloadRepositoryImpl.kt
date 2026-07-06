package tech.acachi.ytplucker.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tech.acachi.ytplucker.domain.DownloadRepository
import tech.acachi.ytplucker.domain.model.DownloadProgress
import tech.acachi.ytplucker.domain.model.VideoMeta

/**
 * Process-wide singleton that bridges the foreground service and the UI. The service writes
 * progress via [publish]; the ViewModel reads [progress]. Because it is an `object`, the flow
 * outlives any Activity/ViewModel, so a download stays observable across config changes and
 * app backgrounding without leaking a Context (none is held here).
 */
object DownloadRepositoryImpl : DownloadRepository {

    private val manager = DownloadManager()

    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    override val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    /** Exposed so the service can drive the engine directly. */
    val downloadManager: DownloadManager get() = manager

    override suspend fun probe(url: String): VideoMeta = manager.probe(url)

    override fun publish(progress: DownloadProgress) {
        _progress.update { it + (progress.jobId to progress) }
    }

    override fun clear(jobId: String) {
        _progress.update { it - jobId }
    }
}
