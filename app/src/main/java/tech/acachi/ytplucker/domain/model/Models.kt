package tech.acachi.ytplucker.domain.model

/**
 * Quality options — mirror the desktop yt-grab dropdown 1:1. Each maps to the exact
 * yt-dlp format selector used by the desktop `download.rs::build_args`.
 */
enum class Quality(val id: String, val label: String) {
    BEST("best", "Best available"),
    P2160("2160", "2160p (4K)"),
    P1440("1440", "1440p"),
    P1080("1080", "1080p"),
    P720("720", "720p"),
    P480("480", "480p"),
    MP3("mp3", "Audio only — MP3"),
    M4A("m4a", "Audio only — M4A");

    val isAudio: Boolean get() = this == MP3 || this == M4A

    companion object {
        fun fromId(id: String): Quality = entries.firstOrNull { it.id == id } ?: BEST
    }
}

/** Lightweight metadata returned by an "Analyze" probe (yt-dlp getInfo). */
data class VideoMeta(
    val title: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val uploader: String?,
)

/** Terminal + in-flight states for a single download job. */
enum class JobStatus { RUNNING, COMPLETED, FAILED, CANCELLED }

/**
 * A snapshot of a download's progress. Immutable so it is safe to publish through a
 * StateFlow and consume in Compose without triggering unstable recompositions.
 */
data class DownloadProgress(
    val jobId: String,
    val title: String,
    val percent: Float,          // 0..100
    val speedBytesPerSec: Float, // bytes/s, -1 when unknown
    val etaSeconds: Long,        // -1 when unknown
    val status: JobStatus,
    val error: String? = null,
)
