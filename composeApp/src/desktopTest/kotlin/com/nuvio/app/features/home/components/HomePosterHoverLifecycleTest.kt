package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
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
import java.io.File
import java.nio.file.Files
import java.awt.Point
import java.awt.Robot
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import androidx.compose.ui.awt.ComposePanel
import kotlinx.coroutines.awaitCancellation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

/** Real hover/popup lifecycle with a controlled suspended resolver; no trailer availability/playback claim. */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class HomePosterHoverLifecycleTest {
    @Test fun desktopDefaultsToSilentHoverTrailerWithExplicitSoundOptIn() {
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

    @Test fun leavingTheRealPopupCancelsResolutionAndReleasesItsSlotWithMotionOff() = runDesktopComposeUiTest(width=1366,height=768) {
        val started = AtomicBoolean()
        val cleanedUp = AtomicBoolean()
        val settings = PosterCardStyleUiState(hoverPreviewEnabled=true, hoverPreviewOpenDelayMillis=500,
            hoverPreviewTrailerEnabled=true, hoverPreviewTrailerSoundEnabled=false)
        runOnIdle { PosterZoomOverlayCoordinator.hide() }
        setContent {
            NuvioTheme(navigationMotion=NavigationMotion.OFF) {
                Box(Modifier.padding(64.dp)) {
                    HomePosterHoverPreview(MetaPreview("fixture:hover", "movie", "Uma história para descobrir"),
                        isWatched=false, onClick={}, onLongClick=null, previewSettings=settings,
                        trailerPreviewSupported=true,
                        trailerResolver={
                            started.set(true)
                            try { awaitCancellation() } finally { cleanedUp.set(true) }
                        }) { hover ->
                        Box(hover.testTag("hover-anchor").size(200.dp,300.dp).background(Color(0xFF18283A))) {
                            Text("Card local")
                        }
                    }
                }
            }
        }
        onNodeWithTag("hover-anchor").performMouseInput { enter(center) }
        waitForIdle()
        waitUntil(timeoutMillis=4_000) { onAllNodesWithTag("home-hover-preview").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("home-hover-preview").performMouseInput { enter(center) }
        waitForIdle()
        waitUntil(timeoutMillis=8_000) { started.get() }
        onNodeWithTag("home-hover-preview").assertIsDisplayed()
        val directory=File("build/reports/telumia-hover-preview").apply { mkdirs() }
        ImageIO.write(onNodeWithTag("home-hover-preview").captureToImage().toAwtImage(), "png", File(directory,"static-pending-preview.png"))
        onNodeWithTag("home-hover-preview").performMouseInput { exit() }
        waitForIdle()
        waitUntil(timeoutMillis=4_000) { cleanedUp.get() && homePosterPreviewOwnership.active.value == null }
        onNodeWithTag("home-hover-preview").assertDoesNotExist()
        runOnIdle { assertTrue(cleanedUp.get()); assertNull(homePosterPreviewOwnership.active.value) }
    }

    @Test fun defaultWindowsHoverPopupReachesTheRealMutedNativeTrailerSurface() {
        assumeTrue("Windows native hover-preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        assumeTrue("popup native player integration is not enabled until its HWND gate passes", AppFeaturePolicy.hoverTrailerPlaybackSupported)
        val fixture = Files.createTempFile("telumia-hover-inline-", ".avi")
        Files.write(fixture, originalAviFixture())
        val resolving = CountDownLatch(1)
        val ready = CountDownLatch(1)
        val failed = CountDownLatch(1)
        val frameCreated = AtomicBoolean(false)
        val originalPointer = java.awt.MouseInfo.getPointerInfo()?.location
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        try {
            assertTrue(AppFeaturePolicy.hoverTrailerPlaybackSupported)
            val defaults = PosterCardStyleUiState()
            assertTrue(defaults.hoverPreviewTrailerEnabled)
            assertFalse(defaults.hoverPreviewTrailerSoundEnabled)
            runCatching { PosterZoomOverlayCoordinator.hide() }
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                window = JFrame("Telumia hover preview with local trailer")
                window.contentPane.add(panel)
                window.setSize(900, 650)
                window.setLocation(100, 100)
                window.isVisible = true
                window.setAlwaysOnTop(true)
                window.toFront()
                window.requestFocus()
                frameCreated.set(true)
                panel.setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            HomePosterHoverPreview(
                                item = MetaPreview("fixture:inline-hover", "movie", "Trailer local"),
                                isWatched = false,
                                onClick = {},
                                onLongClick = null,
                                previewSettings = defaults.copy(hoverPreviewOpenDelayMillis = 500),
                                trailerResolver = {
                                    resolving.countDown()
                                    TrailerPlaybackSource(fixture.toAbsolutePath().toString())
                                },
                                onTrailerReady = { ready.countDown() },
                                onTrailerError = { failed.countDown() },
                            ) { hover ->
                                Box(hover.size(200.dp, 300.dp).background(Color(0xFF18283A))) {
                                    Text("Card local")
                                }
                            }
                        }
                    }
                }
            }
            Thread.sleep(500)
            val panelOrigin = AtomicReference<Point>()
            SwingUtilities.invokeAndWait { panelOrigin.set(panel.locationOnScreen) }
            val robot = Robot().apply { autoDelay = 40 }
            val targetX = panelOrigin.get().x + panel.width / 2
            val targetY = panelOrigin.get().y + panel.height / 2
            robot.mouseMove(panelOrigin.get().x - 20, panelOrigin.get().y - 20)
            robot.waitForIdle()
            robot.mouseMove(targetX, targetY)
            robot.waitForIdle()
            assertTrue(resolving.await(8, TimeUnit.SECONDS), "the real hover must claim preview ownership and resolve the local trailer")
            val trailerReady = ready.await(15, TimeUnit.SECONDS)
            assertTrue(trailerReady, if (failed.count > 0) "the real hover player failed before media-ready" else "the real hover popup must start the muted local trailer")
            Thread.sleep(500)
            assertTrue(homePosterPreviewOwnership.active.value != null, "the popup must retain preview ownership during playback")
        } finally {
            if (frameCreated.get()) {
                SwingUtilities.invokeAndWait {
                    panel.setContent {}
                    panel.dispose()
                    window.dispose()
                }
            }
            if (originalPointer != null) {
                runCatching { Robot().mouseMove(originalPointer.x, originalPointer.y) }
            }
            Files.deleteIfExists(fixture)
        }
    }
}
