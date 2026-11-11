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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
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
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_view_details
import org.jetbrains.compose.resources.stringResource
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
    fun immersiveOverlayConsumesPressAndReleaseBeforeReturningFocus() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val owner = Any()
            val exits = AtomicInteger()
            val entry = HomePosterHoverPreviewPortalEntry(owner, object : PopupPositionProvider {
                override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
                    layoutDirection: LayoutDirection, popupContentSize: IntSize) = IntOffset.Zero
            }, IntRect(0, 0, 1, 1)) { Box(Modifier.size(100.dp)) }
            runOnIdle {
                entry.immersive = true
                entry.onExitImmersive = { exits.incrementAndGet(); entry.immersive = false }
                HomePosterHoverPreviewPortal.show(entry)
            }
            try {
                setContent { HomePosterHoverPreviewOverlayHost() }
                onNodeWithTag("home-hover-overlay-host").performKeyInput {
                    keyDown(Key.Enter)
                    keyUp(Key.Enter)
                }
                runOnIdle {
                    assertTrue(!entry.immersive)
                    assertTrue(exits.get() == 1)
                }
            } finally {
                runOnIdle { HomePosterHoverPreviewPortal.clear(owner) }
            }
        }

    @Test
    fun desktopDefaultsToHoverTrailerWithSound() {
        val defaults = PosterCardStyleUiState()
        assertTrue(defaults.hoverPreviewTrailerSoundEnabled)
        if (DesktopHostOs.current == DesktopHostOs.WINDOWS) {
            assertTrue(AppFeaturePolicy.hoverTrailerPlaybackSupported)
            assertTrue(defaults.hoverPreviewTrailerEnabled)
            assertTrue(AppFeaturePolicy.hoverPreviewUsesRootOverlay)
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
            var detailsActionLabel = ""
            setContent {
                NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                    detailsActionLabel = stringResource(Res.string.home_view_details)
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
            onNodeWithText(detailsActionLabel).assertDoesNotExist()
            onNodeWithTag("home-hover-preview").assertHasClickAction()
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
    fun windowsHoverPreviewReachesTheRealNativeTrailerSurface() {
        assumeTrue("Windows native hover-preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val fixture = Files.createTempFile("telumia-hover-inline-", ".mp4")
        val fixtureBytes = requireNotNull(javaClass.getResourceAsStream("/telumia-hover-audio-test.mp4")) {
            "the deterministic MPEG-4 hover fixture must be on the desktopTest classpath"
        }.use { it.readBytes() }
        Files.write(fixture, fixtureBytes)
        val resolving = CountDownLatch(1)
        val ready = CountDownLatch(1)
        val ended = CountDownLatch(1)
        val immersiveEntered = CountDownLatch(1)
        val chromeRestored = CountDownLatch(1)
        val immersiveWasEntered = AtomicBoolean(false)
        val immersiveEligibilityStates = ConcurrentLinkedQueue<HomePosterHoverImmersiveEligibility>()
        val failed = CountDownLatch(1)
        val trailerReadyCount = AtomicInteger()
        val anchorMeasured = CountDownLatch(1)
        val frameCreated = AtomicBoolean(false)
        val anchorInteractionSource = MutableInteractionSource()
        val testStartedAt = System.nanoTime()
        val lifecycleEvents = ConcurrentLinkedQueue<String>()
        fun recordLifecycleEvent(event: String) {
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - testStartedAt)
            lifecycleEvents.add("${elapsedMs}ms $event")
        }
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        val settings = PosterCardStyleUiState(
            hoverPreviewEnabled = true,
            hoverPreviewOpenDelayMillis = 500,
            hoverPreviewTrailerEnabled = true,
            hoverPreviewTrailerSoundEnabled = true,
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
                                    TrailerPlaybackSource(fixture.toUri().toString())
                                },
                                onTrailerReady = {
                                    recordLifecycleEvent("trailer-ready #${trailerReadyCount.incrementAndGet()}")
                                    ready.countDown()
                                },
                                onTrailerEnded = {
                                    recordLifecycleEvent("trailer-ended")
                                    ended.countDown()
                                },
                                onTrailerError = {
                                    recordLifecycleEvent("trailer-error")
                                    failed.countDown()
                                },
                                onImmersivePreviewChanged = { immersive ->
                                    recordLifecycleEvent("immersive=$immersive")
                                    if (immersive) {
                                        immersiveWasEntered.set(true)
                                        immersiveEntered.countDown()
                                    } else if (immersiveWasEntered.get()) {
                                        chromeRestored.countDown()
                                    }
                                },
                                onImmersiveEligibilityChanged = { state ->
                                    immersiveEligibilityStates.add(state)
                                    recordLifecycleEvent("eligibility=$state")
                                },
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
                if (failed.count == 0L) "the main-window overlay must reach media-ready with the local trailer"
                else "the main-window overlay player reported an error before media-ready",
            )
            assertTrue(
                immersiveEntered.await(8, TimeUnit.SECONDS),
                "the ready local trailer must enter the full-window presentation; " +
                    "eligibility history=${immersiveEligibilityStates.joinToString(" -> ")}; " +
                    "terminal events=${lifecycleEvents.filter { it.contains("trailer-ended") || it.contains("trailer-error") }}; " +
                    "events=${lifecycleEvents.joinToString(" | ")}",
            )
            assertTrue(
                trailerReadyCount.get() == 1,
                "entering the full-window presentation must preserve the already-ready native player",
            )
            Thread.sleep(1_000)
            val screenshot = File("build/reports/telumia-hover-preview/windows-native-overlay.png")
                .apply { parentFile.mkdirs() }
            ImageIO.write(Robot().createScreenCapture(Rectangle(window.locationOnScreen, window.size)), "png", screenshot)
            val robot = Robot()
            robot.mouseMove(window.x + window.width / 2, window.y + window.height / 2)
            assertTrue(
                chromeRestored.await(4, TimeUnit.SECONDS),
                "pointer movement must restore the hover-card chrome; " +
                    "events=${lifecycleEvents.joinToString(" | ")}; " +
                    "focusOwner=${window.focusOwner?.javaClass?.name}; " +
                    "windowFocused=${window.isFocused}; panelFocused=${panel.isFocusOwner}",
            )
            assertTrue(
                ended.await(15, TimeUnit.SECONDS),
                "the full-window trailer must report playback end; events=${lifecycleEvents.joinToString(" | ")}",
            )
            Thread.sleep(1_500)
            assertTrue(
                trailerReadyCount.get() == 1,
                "the same player must finish without remounting after the chrome is restored",
            )
            assertTrue(homePosterPreviewOwnership.active.value != null, "the overlay must retain ownership during playback")
            // The real overlay covers the preview surface; leave it physically before emitting
            // the injected anchor exit so both hover sources are released, as they are on user exit.
            robot.mouseMove(window.x + window.width / 2, window.y + window.height + 8)
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
