package xyrus.code.ytplucker.domain

import kotlinx.coroutines.flow.StateFlow
import xyrus.code.ytplucker.domain.model.DownloadProgress
import xyrus.code.ytplucker.domain.model.VideoMeta

/**
 * The single reactive channel between the download engine / foreground service and the UI.
 *
 * The service owns the lifecycle of a download; it publishes progress here, and both the
 * notification and the ViewModel observe the same [progress] flow. This keeps a download
 * running (and observable) even while the Activity is destroyed.
 */
interface DownloadRepository {

    /** Per-job progress keyed by jobId. Survives config changes and app backgrounding. */
    val progress: StateFlow<Map<String, DownloadProgress>>

    /** Fetch title/thumbnail/duration without downloading. Runs off the main thread. */
    suspend fun probe(url: String): VideoMeta

    /** Called by the service to publish a progress snapshot. */
    fun publish(progress: DownloadProgress)

    /** Drop a finished/cancelled job from the observable map. */
    fun clear(jobId: String)
}
