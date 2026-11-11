package com.nuvio.app.features.trailer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.player.PlayerPlaybackSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.collections_copy_json
import nuvio.composeapp.generated.resources.action_close
import nuvio.composeapp.generated.resources.home_view_details
import nuvio.composeapp.generated.resources.player_diagnostic_error_code
import nuvio.composeapp.generated.resources.player_diagnostic_format_mime
import nuvio.composeapp.generated.resources.player_diagnostic_source_type
import nuvio.composeapp.generated.resources.player_diagnostic_stage
import nuvio.composeapp.generated.resources.player_diagnostics_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class TrailerPlayerDiagnosticsTest {
    private enum class ErrorStage { SourceResolution, PlayerStartup, Playback }

    @Test
    fun errorDetailsCopyOnlySanitizedReportsForTheLocalFailureMatrix() =
        runDesktopComposeUiTest(width = 960, height = 600) {
        data class Scenario(
            val name: String,
            val sourceUrl: String,
            val error: String,
            val stage: ErrorStage,
            val format: String,
            val errorCode: String,
        )

        val scenarios = listOf(
            Scenario(
                name = "progressive mp4",
                sourceUrl = "https://viewer:mp4-password@private.example/private/movie.mp4?token=mp4-signed-secret&account=mp4-account-secret#mp4-fragment",
                error = "HTTP 502 loading the signed mp4; Set-Cookie: session=mp4-cookie-secret; Authorization: Bearer mp4-auth-secret",
                stage = ErrorStage.SourceResolution,
                format = ".mp4",
                errorCode = "HTTP_502",
            ),
            Scenario(
                name = "hls manifest",
                sourceUrl = "https://viewer:hls-password@private.example/private/playlist.m3u8?token=hls-signed-secret&account=hls-account-secret#hls-fragment",
                error = "unsupported format for signed playlist; Set-Cookie: session=hls-cookie-secret",
                stage = ErrorStage.SourceResolution,
                format = "hls (.m3u8)",
                errorCode = "UNSUPPORTED_FORMAT",
            ),
            Scenario(
                name = "range response",
                sourceUrl = "https://viewer:range-password@private.example/private/ranged.mp4?token=range-signed-secret&account=range-account-secret#range-fragment",
                error = "HTTP 416 after Range request; Authorization: Bearer range-auth-secret",
                stage = ErrorStage.Playback,
                format = ".mp4",
                errorCode = "HTTP_416",
            ),
            Scenario(
                name = "missing content length",
                sourceUrl = "https://viewer:length-password@private.example/private/manifest.mpd?token=length-signed-secret&account=length-account-secret#length-fragment",
                error = "request timed out waiting for response body; Set-Cookie: session=length-cookie-secret",
                stage = ErrorStage.PlayerStartup,
                format = "dash (.mpd)",
                errorCode = "TIMEOUT",
            ),
            Scenario(
                name = "slow server",
                sourceUrl = "https://viewer:slow-password@private.example/private/slow.mkv?token=slow-signed-secret&account=slow-account-secret#slow-fragment",
                error = "read timed out; Authorization: Bearer slow-auth-secret",
                stage = ErrorStage.PlayerStartup,
                format = ".mkv",
                errorCode = "TIMEOUT",
            ),
            Scenario(
                name = "interrupted response",
                sourceUrl = "https://viewer:interrupt-password@private.example/private/interrupted.ts?token=interrupt-signed-secret&account=interrupt-account-secret#interrupt-fragment",
                error = "connection reset by peer; Set-Cookie: session=interrupt-cookie-secret",
                stage = ErrorStage.Playback,
                format = ".ts",
                errorCode = "NETWORK_ERROR",
            ),
            Scenario(
                name = "cancelled local playback",
                sourceUrl = "file:///C:/Users/Alice/private/cancelled.mov?account=cancel-account-secret#cancel-fragment",
                error = "playback cancelled by user; Authorization: Bearer cancel-auth-secret",
                stage = ErrorStage.Playback,
                format = ".mov",
                errorCode = "CANCELLED",
            ),
        )
        val clipboard = CapturingClipboardManager()
        val state = TrailerPlaybackState()
        val source = mutableStateOf(TrailerPlaybackSource(scenarios.first().sourceUrl))
        val errorMessage = mutableStateOf<String?>(scenarios.first().error)
        val isLoading = mutableStateOf(false)
        var viewDetailsLabel = ""
        var diagnosticsTitle = ""
        var stageLabel = ""
        var sourceTypeLabel = ""
        var formatLabel = ""
        var errorCodeLabel = ""
        var copyJsonLabel = ""
        var closeLabel = ""

        setContent {
            NuvioTheme {
                val stage = when {
                    errorMessage.value != null -> "source-resolution"
                    state.snapshot.isLoading -> "player-startup"
                    else -> "playback"
                }
                viewDetailsLabel = stringResource(Res.string.home_view_details)
                diagnosticsTitle = stringResource(Res.string.player_diagnostics_title)
                stageLabel = stringResource(Res.string.player_diagnostic_stage, stage)
                sourceTypeLabel = stringResource(
                    Res.string.player_diagnostic_source_type,
                    if (source.value.videoUrl.startsWith("file:")) "file" else "http",
                )
                formatLabel = stringResource(Res.string.player_diagnostic_format_mime, scenarios.firstOrNull {
                    it.sourceUrl == source.value.videoUrl
                }?.format.orEmpty())
                errorCodeLabel = stringResource(
                    Res.string.player_diagnostic_error_code,
                    scenarios.firstOrNull { it.error == (errorMessage.value ?: state.error) }?.errorCode.orEmpty(),
                )
                copyJsonLabel = stringResource(Res.string.collections_copy_json)
                closeLabel = stringResource(Res.string.action_close)
                CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                    TrailerPlayer(
                        source = source.value,
                        state = state,
                        isLoading = isLoading.value,
                        errorMessage = errorMessage.value,
                        onRetry = {},
                    )
                }
            }
        }

        scenarios.forEach { scenario ->
            runOnIdle {
                source.value = TrailerPlaybackSource(scenario.sourceUrl)
                errorMessage.value = scenario.error.takeIf { scenario.stage == ErrorStage.SourceResolution }
                state.error = scenario.error.takeUnless { scenario.stage == ErrorStage.SourceResolution }
                state.snapshot = PlayerPlaybackSnapshot(isLoading = scenario.stage == ErrorStage.PlayerStartup)
                isLoading.value = false
            }
            onNodeWithText(viewDetailsLabel).performClick()
            onNodeWithText(diagnosticsTitle).assertIsDisplayed()
            onNodeWithText(stageLabel).assertIsDisplayed()
            onNodeWithText(sourceTypeLabel).assertIsDisplayed()
            onNodeWithText(formatLabel).assertIsDisplayed()
            onNodeWithText(errorCodeLabel).assertIsDisplayed()
            onNodeWithText(copyJsonLabel).performClick()

            runOnIdle {
                val expectedStage = when (scenario.stage) {
                    ErrorStage.SourceResolution -> "source-resolution"
                    ErrorStage.PlayerStartup -> "player-startup"
                    ErrorStage.Playback -> "playback"
                }
                val expectedSourceType = if (scenario.sourceUrl.startsWith("file:")) "file" else "http"
                val copiedReport = clipboard.copiedText?.text.orEmpty()
                assertEquals(
                    "{\"stage\":\"$expectedStage\",\"source_type\":\"$expectedSourceType\"," +
                        "\"format_or_mime\":\"${scenario.format}\",\"error_code\":\"${scenario.errorCode}\"}",
                    copiedReport,
                    scenario.name,
                )
                listOf(
                    "viewer",
                    "password",
                    "private.example",
                    "/private/",
                    "token=",
                    "signed-secret",
                    "account=",
                    "fragment",
                    "Set-Cookie",
                    "session=",
                    "cookie-secret",
                    "Authorization",
                    "Bearer",
                    "auth-secret",
                    "Alice",
                    "file://",
                ).forEach { marker -> assertFalse(marker in copiedReport, "${scenario.name} report leaked $marker") }
            }
            onNodeWithText(closeLabel).performClick()
        }
    }

    private class CapturingClipboardManager : ClipboardManager {
        var copiedText: AnnotatedString? = null

        override fun getText(): AnnotatedString? = copiedText

        override fun setText(annotatedString: AnnotatedString) {
            copiedText = annotatedString
        }
    }
}
