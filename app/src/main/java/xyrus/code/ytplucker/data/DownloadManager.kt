package xyrus.code.ytplucker.data

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyrus.code.ytplucker.domain.model.Quality
import xyrus.code.ytplucker.domain.model.VideoMeta

/**
 * Thin wrapper over youtubedl-android (yt-dlp + ffmpeg). This is the piece that "pipes the
 * stream to disk": it configures yt-dlp and lets it stream bytes straight to [destDir].
 * The app never holds the video file in the Java heap — yt-dlp owns the socket and the
 * file handle, so heap usage stays flat regardless of file size.
 *
 * The yt-dlp arguments below are a faithful port of the desktop app's
 * `src-tauri/src/download.rs::build_args`.
 */
class DownloadManager {

    /** Fetch metadata only (yt-dlp `-J` under the hood). Safe to call on any thread; hops to IO. */
    suspend fun probe(url: String): VideoMeta = withContext(Dispatchers.IO) {
        val info = YoutubeDL.getInfo(url)
        val heights = info.formats
            ?.mapNotNull { it.height.takeIf { h -> h > 0 } }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
        VideoMeta(
            title = info.title ?: url,
            thumbnailUrl = info.thumbnail,
            durationSeconds = info.duration.takeIf { it > 0 },
            uploader = info.uploader,
            // yt-dlp extractor, e.g. "Youtube", "Twitter" — surfaced so the UI can label
            // the source (YouTube / X). Extraction itself is native to yt-dlp for both.
            source = info.extractorKey?.takeIf { it.isNotBlank() } ?: info.extractor,
            availableHeights = heights,
        )
    }

    /**
     * Run a download to completion. Blocking (yt-dlp runs synchronously) — always called from
     * a background dispatcher inside the foreground service.
     *
     * @param jobId stable id used both to route progress and to cancel via [cancel].
     * @param onProgress invoked with (percent 0..100, etaSeconds, raw yt-dlp line).
     */
    suspend fun download(
        url: String,
        quality: Quality,
        destDir: String,
        jobId: String,
        cookiesPath: String? = null,
        onProgress: (Float, Long, String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply {
            // Output template mirrors desktop: "Title [id].ext".
            addOption("-o", "$destDir/%(title)s [%(id)s].%(ext)s")
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("--newline")
            addOption("--no-config")
            // Numeric options passed as strings for API stability across wrapper versions.
            addOption("--retries", "10")
            addOption("--extractor-retries", "3")
            addOption("--file-access-retries", "3")
            // Bandwidth: parallel fragment fetch + generous socket/chunk sizing.
            addOption("--concurrent-fragments", "4")
            addOption("--buffer-size", "16K")
            addOption("--http-chunk-size", "10M")
            if (cookiesPath != null) {
                addOption("--cookies", cookiesPath)
            }
            applyFormat(quality)
        }
        // ffmpeg (from the youtubedl-android ffmpeg module) is auto-discovered for merging.
        YoutubeDL.execute(request, jobId) { progress, etaInSeconds, line ->
            onProgress(progress, etaInSeconds, line)
        }
    }

    /** Kill the yt-dlp (+ffmpeg) process tree for a job. */
    fun cancel(jobId: String): Boolean = YoutubeDL.destroyProcessById(jobId)

    /**
     * Pull the latest yt-dlp binary (from yt-dlp's own GitHub releases) and persist it.
     * Fixes extraction breakage when the bundled binary has gone stale against site changes.
     * Best-effort — returns false if the update could not complete (e.g. offline).
     */
    suspend fun updateEngine(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching { YoutubeDL.updateYoutubeDL(context) }.isSuccess
    }

    private fun YoutubeDLRequest.applyFormat(quality: Quality) {
        when (quality) {
            Quality.BEST -> {
                addOption("-f", "bv*+ba/b")
                addOption("--merge-output-format", "mp4")
            }
            Quality.P2160, Quality.P1440, Quality.P1080, Quality.P720, Quality.P480 -> {
                val h = quality.id
                // Trailing `/b` falls back to the best available stream when nothing sits at or
                // below the requested height (e.g. a short X clip whose only stream is above the
                // cap), so a download never hard-fails on limited-resolution sources.
                addOption("-f", "bv*[height<=$h]+ba/b[height<=$h]/b")
                addOption("--merge-output-format", "mp4")
            }
            Quality.MP3 -> {
                addOption("-f", "ba/b")
                addOption("-x")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", "0")
            }
            Quality.M4A -> {
                addOption("-f", "ba[ext=m4a]/ba/b")
                addOption("-x")
                addOption("--audio-format", "m4a")
            }
        }
    }
}
