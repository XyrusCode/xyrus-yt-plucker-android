package xyrus.code.ytplucker.domain.model

/**
 * A site the app knows about: its browser card, how to recognise one of its video pages,
 * and how yt-dlp names it. Single source of truth — the browser cards, the download FAB's
 * video-page detection, and [VideoMeta.prettySource] all read from here, so adding a site
 * is one entry rather than three edits that can drift apart.
 *
 * [argb] rather than a Compose `Color` keeps this file free of UI types, so it stays in
 * `domain` and is testable on a plain JVM. The Compose call sites wrap it in `Color(argb)`.
 */
data class SitePlatform(
    val name: String,
    val homeUrl: String,
    val argb: Long,
    val videoPatterns: List<Regex>,
    /** Lowercase fragments matched against yt-dlp's extractor key (e.g. "Youtube", "Twitter"). */
    val extractorKeys: List<String>,
)

val SUPPORTED_PLATFORMS: List<SitePlatform> = listOf(
    SitePlatform(
        name = "YouTube",
        homeUrl = "https://m.youtube.com",
        argb = 0xFFFF0000,
        videoPatterns = listOf(
            Regex("""(?:[\w-]+\.)?youtube\.\w+/(?:watch\?v=|shorts/|live/|embed/)[\w-]+"""),
            Regex("""youtu\.be/[\w-]+"""),
        ),
        extractorKeys = listOf("youtube"),
    ),
    SitePlatform(
        name = "X / Twitter",
        homeUrl = "https://x.com",
        argb = 0xFF1DA1F2,
        videoPatterns = listOf(Regex("""(?:x|twitter)\.\w+/\w+/status/\d+""")),
        extractorKeys = listOf("twitter", "x"),
    ),
    SitePlatform(
        name = "TikTok",
        homeUrl = "https://www.tiktok.com",
        // Brand cyan, not brand black — black is invisible against the app's #0F1115 background.
        argb = 0xFF25F4EE,
        videoPatterns = listOf(
            Regex("""tiktok\.com/@[\w.-]+/video/\d+"""),
            Regex("""tiktok\.com/(?:embed|v)/\d+"""),
            Regex("""(?:vm|vt)\.tiktok\.com/\w+"""),
            Regex("""tiktok\.com/t/\w+"""),
        ),
        extractorKeys = listOf("tiktok"),
    ),
)

/** The platform whose video-page pattern matches [url], or null if this isn't a known video page. */
fun platformForVideoUrl(url: String): SitePlatform? =
    SUPPORTED_PLATFORMS.firstOrNull { p -> p.videoPatterns.any { it.containsMatchIn(url) } }

/**
 * The platform for a yt-dlp extractor key. Exact matches win before substring matches, and
 * single-character keys ("x") are never substring-matched — "x" appears inside almost every
 * extractor key, so a bare `contains` would mislabel unrelated sites as X / Twitter.
 */
fun platformForExtractorKey(key: String): SitePlatform? {
    val k = key.lowercase()
    return SUPPORTED_PLATFORMS.firstOrNull { p -> p.extractorKeys.any { it == k } }
        ?: SUPPORTED_PLATFORMS.firstOrNull { p ->
            p.extractorKeys.any { it.length > 1 && k.contains(it) }
        }
}

private val TIKTOK_HOST = Regex("""^(https?://)(?:m\.|www\.)?tiktok\.com/""", RegexOption.IGNORE_CASE)

private val TIKTOK_PHOTO = Regex("""tiktok\.com/@[\w.-]+/photo/\d+""", RegexOption.IGNORE_CASE)

const val TIKTOK_PHOTO_MSG =
    "TikTok photo/slideshow posts aren't supported — only videos can be downloaded."

/**
 * Canonicalise a URL for yt-dlp. TikTok's extractor requires a literal `www.` on
 * /@user/video/<id>, but the in-app WebView can land on `m.tiktok.com` or the bare domain.
 * Short links (vm./vt./t/) are left alone — their host is already what the extractor wants.
 */
fun normalizeForEngine(url: String): String =
    TIKTOK_HOST.replace(url) { "${it.groupValues[1]}www.tiktok.com/" }

/**
 * A user-facing reason this URL can't be downloaded, or null if it looks fine. TikTok
 * photo/slideshow posts have no video stream and are unsupported by yt-dlp upstream.
 */
fun unsupportedContentReason(url: String): String? =
    if (TIKTOK_PHOTO.containsMatchIn(url)) TIKTOK_PHOTO_MSG else null

/** Thrown when a URL is recognisably something the engine cannot download. */
class UnsupportedContentException(message: String) : Exception(message)
