package xyrus.code.ytplucker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformsTest {

    @Test
    fun `recognises youtube video pages`() {
        assertEquals("YouTube", platformForVideoUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ")?.name)
        assertEquals("YouTube", platformForVideoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")?.name)
        assertEquals("YouTube", platformForVideoUrl("https://youtu.be/dQw4w9WgXcQ")?.name)
    }

    @Test
    fun `recognises youtube shorts`() {
        assertEquals("YouTube", platformForVideoUrl("https://www.youtube.com/shorts/abc123XYZ")?.name)
    }

    @Test
    fun `recognises x status pages`() {
        assertEquals("X / Twitter", platformForVideoUrl("https://x.com/someone/status/1234567890")?.name)
        assertEquals("X / Twitter", platformForVideoUrl("https://twitter.com/someone/status/1234567890")?.name)
    }

    @Test
    fun `recognises tiktok video urls in every form`() {
        assertEquals("TikTok", platformForVideoUrl("https://www.tiktok.com/@user.name/video/7123456789")?.name)
        assertEquals("TikTok", platformForVideoUrl("https://m.tiktok.com/@user/video/7123456789")?.name)
        assertEquals("TikTok", platformForVideoUrl("https://vm.tiktok.com/ZM123abc")?.name)
        assertEquals("TikTok", platformForVideoUrl("https://vt.tiktok.com/ZM123abc")?.name)
        assertEquals("TikTok", platformForVideoUrl("https://www.tiktok.com/t/ZM123abc")?.name)
        assertEquals("TikTok", platformForVideoUrl("https://www.tiktok.com/embed/7123456789")?.name)
    }

    @Test
    fun `non-video pages are not downloadable`() {
        assertNull(platformForVideoUrl("https://example.com"))
        assertNull(platformForVideoUrl("https://www.youtube.com"))
        assertNull(platformForVideoUrl("https://www.tiktok.com/@someuser"))
    }

    /** The FAB must not appear on a photo post — it can never succeed. */
    @Test
    fun `tiktok photo posts are not video pages`() {
        assertNull(platformForVideoUrl("https://www.tiktok.com/@user/photo/7123456789"))
    }

    @Test
    fun `tiktok hosts are normalised to www for the extractor`() {
        val expected = "https://www.tiktok.com/@user/video/7123456789"
        assertEquals(expected, normalizeForEngine("https://m.tiktok.com/@user/video/7123456789"))
        assertEquals(expected, normalizeForEngine("https://tiktok.com/@user/video/7123456789"))
        assertEquals(expected, normalizeForEngine(expected))
    }

    @Test
    fun `normalising leaves short links and other sites alone`() {
        val vm = "https://vm.tiktok.com/ZM123abc"
        assertEquals(vm, normalizeForEngine(vm))
        val yt = "https://m.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals(yt, normalizeForEngine(yt))
    }

    @Test
    fun `photo posts report a reason and videos do not`() {
        assertEquals(
            TIKTOK_PHOTO_MSG,
            unsupportedContentReason("https://www.tiktok.com/@user/photo/7123456789"),
        )
        assertNull(unsupportedContentReason("https://www.tiktok.com/@user/video/7123456789"))
        assertNull(unsupportedContentReason("https://example.com"))
    }

    @Test
    fun `extractor keys map to display names`() {
        assertEquals("YouTube", platformForExtractorKey("Youtube")?.name)
        assertEquals("YouTube", platformForExtractorKey("youtube:tab")?.name)
        assertEquals("X / Twitter", platformForExtractorKey("Twitter")?.name)
        assertEquals("X / Twitter", platformForExtractorKey("x")?.name)
        assertEquals("TikTok", platformForExtractorKey("TikTok")?.name)
    }

    /** "x" must only ever match exactly — as a substring it is inside almost every key. */
    @Test
    fun `single-character keys never substring-match`() {
        assertNull(platformForExtractorKey("Xhamster"))
        assertNull(platformForExtractorKey("vimeo"))
    }

    @Test
    fun `web schemes are recognised and others are not`() {
        assertTrue(isWebUrl("https://www.tiktok.com/@u/video/1"))
        assertTrue(isWebUrl("http://example.com"))
        assertFalse(isWebUrl("snssdk1340://aweme/detail/123"))
        assertFalse(isWebUrl("intent://scan/#Intent;scheme=zxing;end"))
    }

    /** The exact deep-link that dead-ended the WebView with ERR_UNKNOWN_URL_SCHEME. */
    @Test
    fun `tiktok app link yields the real web url`() {
        val deepLink = "snssdk1340://aweme/detail/7634514435054308628?" +
            "insert_feed=1&params_url=https%3A%2F%2Fwww.tiktok.com%2F%40user%2Fvideo%2F7634514435054308628"
        assertEquals(
            "https://www.tiktok.com/@user/video/7634514435054308628",
            webFallbackFromAppLink(deepLink),
        )
    }

    /** intent:// delimits its extras with semicolons, not & — and `;end` must not leak in. */
    @Test
    fun `intent links yield their browser fallback`() {
        val intentLink = "intent://www.tiktok.com/@u/video/1#Intent;scheme=https;" +
            "S.browser_fallback_url=https%3A%2F%2Fwww.tiktok.com%2F%40u%2Fvideo%2F1;end"
        assertEquals("https://www.tiktok.com/@u/video/1", webFallbackFromAppLink(intentLink))
    }

    @Test
    fun `app links without a fallback yield null`() {
        assertNull(webFallbackFromAppLink("snssdk1340://aweme/detail/7634514435054308628"))
        assertNull(webFallbackFromAppLink("https://www.tiktok.com/@u/video/1"))
    }

    /** A fallback pointing at another app scheme must not be handed back to the WebView. */
    @Test
    fun `non-web fallbacks are rejected`() {
        assertNull(webFallbackFromAppLink("intent://x#Intent;S.browser_fallback_url=market%3A%2F%2Fdetails;end"))
    }
}
