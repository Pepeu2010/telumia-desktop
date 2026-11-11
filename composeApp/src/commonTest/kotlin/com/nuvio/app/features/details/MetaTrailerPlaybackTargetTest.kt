package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MetaTrailerPlaybackTargetTest {

    @Test
    fun `youtube id resolves to a YouTube target and thumbnail`() {
        val trailer = trailer(site = "YouTube", key = "TESTtest123")

        val target = assertIs<MetaTrailerPlaybackTarget.YouTube>(resolveTrailerPlaybackTarget(trailer))

        assertEquals("TESTtest123", target.videoId)
        assertEquals("https://www.youtube.com/watch?v=TESTtest123", target.url)
        assertEquals("https://img.youtube.com/vi/TESTtest123/hqdefault.jpg", trailer.youtubeThumbnailUrl())
    }

    @Test
    fun `YouTube short URL resolves its video id`() {
        val trailer = trailer(site = "YouTube", key = "https://youtu.be/TESTtest123")

        val target = assertIs<MetaTrailerPlaybackTarget.YouTube>(resolveTrailerPlaybackTarget(trailer))

        assertEquals("TESTtest123", target.videoId)
    }

    @Test
    fun `YouTube provider rejects a URL hosted by another provider`() {
        val trailer = trailer(
            site = "YouTube",
            key = "https://media.example.test/trailers/clip-1",
        )

        assertNull(resolveTrailerPlaybackTarget(trailer))
        assertNull(trailer.youtubeThumbnailUrl())
    }

    @Test
    fun `external provider URL is preserved without being relabeled as YouTube`() {
        val url = "https://media.example.test/trailers/clip-1"
        val trailer = trailer(site = "Partner Catalog", key = url)

        assertEquals(
            MetaTrailerPlaybackTarget.External(site = "Partner Catalog", url = url),
            resolveTrailerPlaybackTarget(trailer),
        )
        assertNull(trailer.youtubeThumbnailUrl())
    }

    @Test
    fun `vimeo identifier is converted to a Vimeo destination`() {
        val trailer = trailer(site = "Vimeo", key = "000000000000000000000000")

        assertEquals(
            MetaTrailerPlaybackTarget.External(
                site = "Vimeo",
                url = "https://vimeo.com/000000000000000000000000",
            ),
            resolveTrailerPlaybackTarget(trailer),
        )
    }

    @Test
    fun `external provider URL with a mismatched known host is rejected`() {
        val trailer = trailer(
            site = "Vimeo",
            key = "https://media.example.test/trailers/clip-1",
        )

        assertNull(resolveTrailerPlaybackTarget(trailer))
    }

    @Test
    fun `unsupported non-YouTube identifier is rejected`() {
        assertNull(resolveTrailerPlaybackTarget(trailer(site = "Partner Catalog", key = "clip-1")))
    }

    private fun trailer(site: String, key: String) = MetaTrailer(
        id = "trailer-fixture",
        key = key,
        name = "Trailer fixture",
        site = site,
    )
}
