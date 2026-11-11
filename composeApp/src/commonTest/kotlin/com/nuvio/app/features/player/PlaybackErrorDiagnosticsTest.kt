package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackErrorDiagnosticsTest {
    @Test
    fun `report includes only safe source facts and classified error`() {
        val signedUrl = "https://viewer:secret@cdn.example/private/movie.mp4?token=signed-token&account=viewer%40example.com"
        val report = playbackErrorDiagnostics(
            stage = PlaybackDiagnosticStage.PlayerStartup,
            sourceUrl = signedUrl,
            streamType = "http",
            responseHeaders = mapOf(
                "Content-Type" to "video/mp4; charset=utf-8",
                "Set-Cookie" to "session=account-cookie-secret",
                "Authorization" to "Bearer account-access-token",
            ),
            errorMessage = "HTTP 403 while loading $signedUrl; Bearer failure-token; user@example.com",
        )

        assertEquals(PlaybackDiagnosticStage.PlayerStartup, report.stage)
        assertEquals("http", report.sourceType)
        assertEquals("video/mp4", report.formatOrMime)
        assertEquals("HTTP_403", report.errorCode)

        val json = report.toSafeJson()
        listOf(
            "viewer",
            "secret",
            "cdn.example",
            "/private/movie.mp4",
            "token=",
            "signed-token",
            "account",
            "example.com",
            "session=",
            "cookie-secret",
            "Authorization",
            "access-token",
            "failure-token",
        ).forEach { secret -> assertFalse(secret in json, "report leaked $secret") }
        assertTrue("\"source_type\":\"http\"" in json)
        assertTrue("\"format_or_mime\":\"video/mp4\"" in json)
    }

    @Test
    fun `format discovery ignores query fragments and untrusted type labels`() {
        val report = playbackErrorDiagnostics(
            stage = PlaybackDiagnosticStage.Playback,
            sourceUrl = "https://cdn.example/video.m3u8?name=account-secret#token-fragment",
            streamType = "account@example.com",
            responseHeaders = mapOf("Content-Type" to "application/octet-stream; token=secret"),
            errorMessage = "timeout for https://cdn.example/video.m3u8?token=secret",
        )

        assertEquals("hls (.m3u8)", report.formatOrMime)
        assertEquals("http", report.sourceType)
        assertEquals("TIMEOUT", report.errorCode)
        assertFalse("account" in report.toSafeJson())
        assertFalse("secret" in report.toSafeJson())
        assertFalse("cdn.example" in report.toSafeJson())
    }

    @Test
    fun `safe error token never echoes arbitrary provider text`() {
        val unsafe = "Failed https://user:pw@host.invalid/movie.mkv?token=private with alice@example.com cookie=session-secret"

        val token = safePlaybackErrorToken(unsafe)

        assertEquals("UNKNOWN", token)
        assertFalse("host.invalid" in token.orEmpty())
        assertFalse("private" in token.orEmpty())
        assertFalse("alice@example.com" in token.orEmpty())
        assertFalse("session-secret" in token.orEmpty())
    }

    @Test
    fun `desktop error tokens preserve only allowlisted error codes`() {
        val cases = listOf(
            "connection refused" to "NETWORK_ERROR",
            "unsupported format" to "UNSUPPORTED_FORMAT",
            "request timed out" to "TIMEOUT",
            "playback cancelled" to "CANCELLED",
            "HTTP 404 while loading a signed source" to "HTTP_404",
        )

        cases.forEach { (rawError, expectedCode) ->
            val safeToken = safePlaybackErrorToken(rawError)
            assertEquals(expectedCode, safeToken)
            assertEquals(expectedCode, playbackErrorCode(safeToken))
            assertEquals(
                expectedCode,
                playbackErrorDiagnostics(
                    stage = PlaybackDiagnosticStage.PlayerStartup,
                    sourceUrl = "https://media.example/video.mp4?token=private",
                    streamType = "http",
                    responseHeaders = emptyMap(),
                    errorMessage = safeToken,
                ).errorCode,
            )
        }
    }

    @Test
    fun `local playback scenario matrix serializes only allowlisted facts`() {
        data class Scenario(
            val name: String,
            val stage: PlaybackDiagnosticStage,
            val sourceUrl: String,
            val streamType: String?,
            val responseHeaders: Map<String, String>,
            val errorMessage: String,
            val sourceType: String,
            val format: String,
            val errorCode: String,
        )

        val scenarios = listOf(
            Scenario(
                name = "progressive mp4",
                stage = PlaybackDiagnosticStage.PlayerStartup,
                sourceUrl = "https://viewer:mp4-secret@fixture.invalid/private/movie.mp4?token=mp4-signed-secret",
                streamType = "http",
                responseHeaders = mapOf(
                    "Content-Type" to "video/mp4; charset=utf-8",
                    "Set-Cookie" to "session=mp4-cookie-secret",
                    "Authorization" to "Bearer mp4-auth-secret",
                ),
                errorMessage = "HTTP 502 for https://fixture.invalid/private/movie.mp4?token=mp4-signed-secret",
                sourceType = "http",
                format = "video/mp4",
                errorCode = "HTTP_502",
            ),
            Scenario(
                name = "hls manifest",
                stage = PlaybackDiagnosticStage.PlayerStartup,
                sourceUrl = "https://fixture.invalid/private/playlist.m3u8?sig=hls-signed-secret#private-fragment",
                streamType = "hls",
                responseHeaders = mapOf("Set-Cookie" to "session=hls-cookie-secret"),
                errorMessage = "unsupported format for playlist https://fixture.invalid/private/playlist.m3u8?sig=hls-signed-secret",
                sourceType = "http",
                format = "hls",
                errorCode = "UNSUPPORTED_FORMAT",
            ),
            Scenario(
                name = "range response",
                stage = PlaybackDiagnosticStage.Playback,
                sourceUrl = "https://fixture.invalid/private/ranged.mp4?signature=range-signed-secret",
                streamType = "http",
                responseHeaders = mapOf(
                    "Content-Type" to "video/mp4",
                    "Content-Range" to "bytes 0-99/1000; token=range-header-secret",
                    "Set-Cookie" to "session=range-cookie-secret",
                ),
                errorMessage = "HTTP 416 while reading https://fixture.invalid/private/ranged.mp4?signature=range-signed-secret",
                sourceType = "http",
                format = "video/mp4",
                errorCode = "HTTP_416",
            ),
            Scenario(
                name = "missing content length",
                stage = PlaybackDiagnosticStage.PlayerStartup,
                sourceUrl = "https://fixture.invalid/private/manifest.mpd?token=length-signed-secret",
                streamType = "dash",
                responseHeaders = mapOf("Content-Type" to "application/octet-stream; token=length-header-secret"),
                errorMessage = "request timed out for https://fixture.invalid/private/manifest.mpd?token=length-signed-secret",
                sourceType = "http",
                format = "dash",
                errorCode = "TIMEOUT",
            ),
            Scenario(
                name = "slow server",
                stage = PlaybackDiagnosticStage.PlayerStartup,
                sourceUrl = "https://fixture.invalid/private/slow.mkv?token=slow-signed-secret",
                streamType = "http",
                responseHeaders = mapOf("Set-Cookie" to "session=slow-cookie-secret"),
                errorMessage = "source timed out: https://fixture.invalid/private/slow.mkv?token=slow-signed-secret",
                sourceType = "http",
                format = ".mkv",
                errorCode = "TIMEOUT",
            ),
            Scenario(
                name = "interrupted response",
                stage = PlaybackDiagnosticStage.Playback,
                sourceUrl = "https://fixture.invalid/private/interrupted.ts?token=interrupt-signed-secret",
                streamType = "http",
                responseHeaders = mapOf("Content-Type" to "video/mp2t"),
                errorMessage = "connection reset while reading https://fixture.invalid/private/interrupted.ts?token=interrupt-signed-secret",
                sourceType = "http",
                format = "video/mp2t",
                errorCode = "NETWORK_ERROR",
            ),
            Scenario(
                name = "cancelled local playback",
                stage = PlaybackDiagnosticStage.Playback,
                sourceUrl = "file:///C:/Users/Alice/private/cancelled.mov?account=cancel-account-secret",
                streamType = null,
                responseHeaders = mapOf("Set-Cookie" to "session=cancel-cookie-secret"),
                errorMessage = "playback cancelled for file:///C:/Users/Alice/private/cancelled.mov?account=cancel-account-secret",
                sourceType = "file",
                format = ".mov",
                errorCode = "CANCELLED",
            ),
        )

        scenarios.forEach { scenario ->
            val diagnostics = playbackErrorDiagnostics(
                stage = scenario.stage,
                sourceUrl = scenario.sourceUrl,
                streamType = scenario.streamType,
                responseHeaders = scenario.responseHeaders,
                errorMessage = scenario.errorMessage,
            )

            assertEquals(scenario.sourceType, diagnostics.sourceType, scenario.name)
            assertEquals(scenario.format, diagnostics.formatOrMime, scenario.name)
            assertEquals(scenario.errorCode, diagnostics.errorCode, scenario.name)
            val json = diagnostics.toSafeJson()
            assertEquals(
                "{\"stage\":\"${scenario.stage.value}\",\"source_type\":\"${scenario.sourceType}\"," +
                    "\"format_or_mime\":\"${scenario.format}\",\"error_code\":\"${scenario.errorCode}\"}",
                json,
                scenario.name,
            )
            listOf(
                "fixture.invalid",
                "private",
                "token=",
                "signature=",
                "fragment",
                "session=",
                "Set-Cookie",
                "Authorization",
                "Bearer",
                "secret",
                "Alice",
                "account=",
            ).forEach { marker -> assertFalse(marker in json, "${scenario.name} report leaked $marker") }
            assertFalse("https://" in json, "${scenario.name} report must not contain a URL")
            assertFalse("file://" in json, "${scenario.name} report must not contain a file path")
        }
    }
}
