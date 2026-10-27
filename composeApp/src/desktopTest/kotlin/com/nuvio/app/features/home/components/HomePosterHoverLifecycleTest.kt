package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.core.ui.PosterZoomOverlayCoordinator
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.originalAviFixture
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlinx.coroutines.awaitCancellation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

/** Real hover/overlay lifecycle; controlled resolver test makes no trailer availability claim. */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class HomePosterHoverLifecycleTest {
    @Test
    fun desktopDefaultsToSilentHoverTrailerWithExplicitSoundOptIn() {
        val defaults = PosterCardStyleUiState()
        assertFalse(defaults.hoverPreviewTrailerSoundEnabled)
        if (DesktopHostOs.current == DesktopHostOs.WINDOWS) {
            assertFalse(AppFeaturePolicy.hoverTrailerPlaybackSupported)
            assertFalse(defaults.hoverPreviewTrailerEnabled)
        } else {
            assertTrue(AppFeaturePolicy.hoverTrailerPlaybackSupported)
            assertTrue(defaults.hoverPreviewTrailerEnabled)
        }
    }

    @Test
    fun leavingTheRealPopupCancelsResolutionAndReleasesItsSlotWithMotionOff() =
        runDesktopComposeUiTest(width = 1366, height = 768) {
            val started = AtomicBoolean()
            val cleanedUp = AtomicBoolean()
            val settings = PosterCardStyleUiState(
                hoverPreviewEnabled = true,
                hoverPreviewOpenDelayMillis = 500,
                hoverPreviewTrailerEnabled = true,
                hoverPreviewTrailerSoundEnabled = false,
            )
            runOnIdle { PosterZoomOverlayCoordinator.hide() }
            setContent {
                NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.padding(64.dp)) {
                            HomePosterHoverPreview(
                                MetaPreview("fixture:hover", "movie", "Uma história para descobrir"),
                                isWatched = false,
                                onClick = {},
                                onLongClick = null,
                                previewSettings = settings,
                                trailerPreviewSupported = true,
                                trailerResolver = {
                                    started.set(true)
                                    try {
                                        awaitCancellation()
                                    } finally {
                                        cleanedUp.set(true)
                                    }
                                },
                            ) { hover ->
                                Box(
                                    hover.testTag("hover-anchor")
                                        .size(200.dp, 300.dp)
                                        .background(Color(0xFF18283A)),
                                ) {
                                    Text("Card local")
                                }
                            }
                        }
                        HomePosterHoverPreviewOverlayHost()
                    }
                }
            }
            onNodeWithTag("hover-anchor").performMouseInput { enter(center) }
            waitForIdle()
            waitUntil(timeoutMillis = 4_000) {
                onAllNodesWithTag("home-hover-preview").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("home-hover-preview").performMouseInput { moveTo(center) }
            waitForIdle()
            waitUntil(timeoutMillis = 8_000) { started.get() }
            onNodeWithTag("home-hover-preview").assertIsDisplayed()
            val screenshotDirectory = File("build/reports/telumia-hover-preview").apply { mkdirs() }
            ImageIO.write(
                onNodeWithTag("home-hover-preview").captureToImage().toAwtImage(),
                "png",
                File(screenshotDirectory, "static-pending-preview.png"),
            )
            onNodeWithTag("home-hover-preview").performMouseInput { exit() }
            waitForIdle()
            waitUntil(timeoutMillis = 4_000) {
                cleanedUp.get() && homePosterPreviewOwnership.active.value == null
            }
            onNodeWithTag("home-hover-preview").assertDoesNotExist()
            runOnIdle {
                assertTrue(cleanedUp.get())
                assertNull(homePosterPreviewOwnership.active.value)
            }
        }

    @Test
    fun windowsHoverPreviewReachesTheRealMutedNativeTrailerSurface() {
        assumeTrue("Windows native hover-preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val fixture = Files.createTempFile("telumia-hover-inline-", ".avi")
        Files.write(fixture, originalAviFixture())
        val resolving = CountDownLatch(1)
        val ready = CountDownLatch(1)
        val failed = CountDownLatch(1)
        val anchorMeasured = CountDownLatch(1)
        val frameCreated = AtomicBoolean(false)
        val anchorInteractionSource = MutableInteractionSource()
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        val settings = PosterCardStyleUiState(
            hoverPreviewEnabled = true,
            hoverPreviewOpenDelayMillis = 500,
            hoverPreviewTrailerEnabled = true,
            hoverPreviewTrailerSoundEnabled = false,
        )

        try {
            runCatching { PosterZoomOverlayCoordinator.hide() }
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                window = JFrame("Telumia hover preview with local trailer")
                window.contentPane.add(panel)
                window.setSize(900, 650)
                window.setLocation(100, 100)
                frameCreated.set(true)
                window.isVisible = true
                window.isAlwaysOnTop = true
                window.toFront()
                window.requestFocus()
                panel.setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            HomePosterHoverPreview(
                                item = MetaPreview("fixture:inline-hover", "movie", "Trailer local"),
                                isWatched = false,
                                onClick = {},
                                onLongClick = null,
                                previewSettings = settings,
                                trailerPreviewSupported = true,
                                anchorInteractionSourceOverride = anchorInteractionSource,
                                trailerResolver = {
                                    resolving.countDown()
                                    TrailerPlaybackSource(fixture.toAbsolutePath().toString())
                                },
                                onTrailerReady = { ready.countDown() },
                                onTrailerError = { failed.countDown() },
                            ) { hover ->
                                Box(
                                    hover.testTag("windows-hover-anchor")
                                        .size(200.dp, 300.dp)
                                        .background(Color(0xFF18283A))
                                        .onGloballyPositioned { coordinates ->
                                            anchorMeasured.countDown()
                                        },
                                ) {
                                    Text("Card local")
                                }
                            }
                            HomePosterHoverPreviewOverlayHost()
                        }
                    }
                }
            }

            assertTrue(anchorMeasured.await(8, TimeUnit.SECONDS), "ComposePanel must lay out the actual hover anchor")
            val hoverEnter = HoverInteraction.Enter()
            assertTrue(anchorInteractionSource.tryEmit(hoverEnter), "the test must enter the real composed hover path")

            assertTrue(
                resolving.await(8, TimeUnit.SECONDS),
                "the composed hover path must claim preview ownership and resolve the local trailer",
            )
            val trailerReady = ready.await(15, TimeUnit.SECONDS)
            assertTrue(
                trailerReady,
                if (failed.count == 0L) "the main-window overlay must reach media-ready with the muted local trailer"
                else "the main-window overlay player reported an error before media-ready",
            )
            Thread.sleep(250)
            assertTrue(homePosterPreviewOwnership.active.value != null, "the overlay must retain ownership during playback")
            val screenshot = File("build/reports/telumia-hover-preview/windows-native-overlay.png")
                .apply { parentFile.mkdirs() }
            ImageIO.write(Robot().createScreenCapture(Rectangle(window.locationOnScreen, window.size)), "png", screenshot)
            assertTrue(anchorInteractionSource.tryEmit(HoverInteraction.Exit(hoverEnter)))
            val releaseDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4)
            while (homePosterPreviewOwnership.active.value != null && System.nanoTime() < releaseDeadline) {
                Thread.sleep(25)
            }
            assertNull(homePosterPreviewOwnership.active.value, "leaving the hover source must release the overlay and native decoder")
        } finally {
            if (frameCreated.get()) {
                SwingUtilities.invokeAndWait {
                    panel.setContent {}
                    panel.dispose()
                    window.dispose()
                }
            }
            val deleteDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (Files.exists(fixture) && System.nanoTime() < deleteDeadline) {
                runCatching { Files.deleteIfExists(fixture) }
                if (Files.exists(fixture)) Thread.sleep(50)
            }
            assertFalse(Files.exists(fixture), "the local trailer fixture must be released after playback teardown")
        }
    }
}
