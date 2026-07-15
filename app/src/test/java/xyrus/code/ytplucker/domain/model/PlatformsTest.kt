package xyrus.code.ytplucker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
