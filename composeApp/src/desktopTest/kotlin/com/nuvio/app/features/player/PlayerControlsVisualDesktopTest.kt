package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PlayerControlsVisualDesktopTest {
    @Test fun controlsRemainUsableAtDesktopAndCompactSizes() {
        for ((width, height) in listOf(1366 to 768, 800 to 600)) {
            var toggles = 0
            runDesktopComposeUiTest(width = width, height = height) {
                setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF506D72), Color(0xFF17252D), Color(0xFF080E12)))).testTag("player-frame")) {
                            PlayerControlsShell(
                                title = "Uma jornada pelo cinema", streamTitle = "", providerName = "",
                                seasonNumber = 1, episodeNumber = 2, episodeTitle = "A chegada",
                                playbackSnapshot = PlayerPlaybackSnapshot(isLoading = false, isPlaying = true,
                                    durationMs = 6_000_000, positionMs = 2_200_000, bufferedPositionMs = 3_000_000),
                                displayedPositionMs = 2_200_000, metrics = PlayerLayoutMetrics.fromWidth(width.dp),
                                resizeMode = PlayerResizeMode.Fit, isLocked = false, horizontalSafePadding = 0.dp,
                                onLockToggle = {}, onBack = {}, onTogglePlayback = { toggles++ },
                                onSeekBack = {}, onSeekForward = {}, onResizeModeClick = {}, onSpeedClick = {},
                                onSubtitleClick = {}, onAudioClick = {}, onScrubChange = {}, onScrubFinished = {},
                            )
                        }
                    }
                }
                onNodeWithText("Uma jornada pelo cinema").assertIsDisplayed()
                onNodeWithTag("player-play-pause").performClick()
                runOnIdle { assertEquals(1, toggles) }
                val image = onNodeWithTag("player-frame").captureToImage().toAwtImage()
                val output = File("build/telumia-player-visual/player-${width}x${height}.png").also { it.parentFile.mkdirs() }
                ImageIO.write(image, "png", output)
            }
        }
    }
}
