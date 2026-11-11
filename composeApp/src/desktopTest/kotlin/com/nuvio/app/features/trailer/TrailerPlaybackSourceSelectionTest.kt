package com.nuvio.app.features.trailer

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrailerPlaybackSourceSelectionTest {
    private fun candidate(url: String, mime: String) = StreamCandidate(
        client = "fixture", priority = 1, url = url, score = 1.0, bitrate = 1_000,
        mimeType = mime, hasN = false, height = 720, fps = 30,
        ext = if (mime.startsWith("audio/")) "m4a" else "mp4",
    )

    @Test fun rejectsVideoOnlyWhenNoAudioOrCombinedSourceExists() = runBlocking {
        val video = candidate("https://media.example/video", "video/mp4")
        assertNull(TrailerExtractionPlatform.buildPlaybackSource(null, null, video, null))
    }

    @Test fun pairsSeparateVideoAndAudio() = runBlocking {
        val video = candidate("https://media.example/video", "video/mp4")
        val audio = candidate("https://media.example/audio", "audio/mp4")
        assertEquals(TrailerPlaybackSource(video.url, audio.url),
            TrailerExtractionPlatform.buildPlaybackSource(null, null, video, audio))
    }

    @Test fun fallsBackToCombinedSourceWhenSeparateAudioIsMissing() = runBlocking {
        val video = candidate("https://media.example/video", "video/mp4")
        val combined = candidate("https://media.example/combined", "video/mp4")
        assertEquals(TrailerPlaybackSource(combined.url),
            TrailerExtractionPlatform.buildPlaybackSource(null, combined, video, null))
    }

    @Test fun retainsCombinedSourceForPlaybackFailureOfSeparateAudio() = runBlocking {
        val video = candidate("https://media.example/video", "video/mp4")
        val audio = candidate("https://media.example/audio", "audio/mp4")
        val combined = candidate("https://media.example/combined", "video/mp4")
        assertEquals(TrailerPlaybackSource(video.url, audio.url, fallbackVideoUrl = combined.url),
            TrailerExtractionPlatform.buildPlaybackSource(null, combined, video, audio))
    }
}
