package xyrus.code.ytplucker.service

/**
 * Parsers for yt-dlp's `--newline` stdout and small display formatters. Kept in the service
 * package because the raw-line parsing is a service concern; the UI formats from the typed
 * [xyrus.code.ytplucker.domain.model.DownloadProgress] instead.
 *
 * A yt-dlp download line looks like:
 *   [download]  45.2% of 12.34MiB at 2.50MiB/s ETA 00:03
 */

private val SPEED_REGEX =
    Regex("""at\s+([\d.]+)\s*([KMG]?i?B)/s""", RegexOption.IGNORE_CASE)

private val DEST_REGEX = Regex("""\[download]\s+Destination:\s+(.+)""")

/** Returns bytes/sec parsed from a yt-dlp line, or null when the line has no speed token. */
fun parseSpeedBytesPerSec(line: String): Float? {
    val m = SPEED_REGEX.find(line) ?: return null
    val value = m.groupValues[1].toFloatOrNull() ?: return null
    return value * unitMultiplier(m.groupValues[2])
}

/** Best-effort title from a "Destination:" line (filename without extension). */
fun parseTitle(line: String): String? {
    val dest = DEST_REGEX.find(line)?.groupValues?.get(1)?.trim() ?: return null
    return dest.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
}

/** Placeholder title derived from a URL before the first progress line arrives. */
fun shortTitleFrom(url: String): String =
    url.substringAfter("://").substringBefore('/').ifBlank { "Download" }

/** "2.50 MB/s" style formatting; returns "—" when unknown. */
fun formatSpeed(bytesPerSec: Float): String {
    if (bytesPerSec <= 0f) return "—"
    val mb = bytesPerSec / (1024f * 1024f)
    return if (mb >= 1f) String.format("%.2f MB/s", mb)
    else String.format("%.0f KB/s", bytesPerSec / 1024f)
}

/**
 * Heuristic: does this yt-dlp error look like the bundled binary has gone stale against a site's
 * changes (as opposed to a genuine bad URL / network error)? These are the signatures that a
 * `updateYoutubeDL()` + retry typically resolves.
 */
fun isStaleEngineError(message: String?): Boolean {
    val m = message?.lowercase() ?: return false
    return listOf(
        "precondition check failed",
        "unable to extract",
        "http error 400",
        "http error 403",
        "confirm you are on the latest version",
        "nsig extraction failed",
        "unable to download api page",
        "sign in to confirm",
        "please report this issue",
        "requested format is not available",
        "no video formats found",
        "format not found",
        "incomplete data received",
        "unable to download video",
    ).any { m.contains(it) }
}

private fun unitMultiplier(unit: String): Float = when (unit.uppercase().replace("I", "")) {
    "GB" -> 1024f * 1024f * 1024f
    "MB" -> 1024f * 1024f
    "KB" -> 1024f
    else -> 1f
}
