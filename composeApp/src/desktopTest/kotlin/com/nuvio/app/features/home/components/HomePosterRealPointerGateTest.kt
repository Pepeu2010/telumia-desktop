package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.core.ui.PosterZoomOverlayCoordinator
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.NativePlayerBridge
import com.nuvio.app.features.player.desktop.NativePlayerController
import com.nuvio.app.features.player.desktop.NativePlayerHost
import com.nuvio.app.features.player.desktop.originalAviFixture
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import java.awt.Component
import java.awt.Container
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

/** Interactive Windows gate: no InteractionSource injection or synthetic Compose mouse input. */
@OptIn(ExperimentalComposeUiApi::class)
class HomePosterRealPointerGateTest {
    @Test
    fun robotTransfersToVisibleMutedNativeVideoAndLeavingReleasesItsDecoder() {
        assumeTrue(DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val evidence = File("build/reports/phase3-os-pointer").apply { mkdirs() }
        val log = File(evidence, "compose-gate.log").apply { writeText("") }
        val startedAt = System.nanoTime()
        fun record(message: String) {
            val timed = "${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)}ms $message"
            log.appendText("$timed\n"); println(timed)
        }
        val fixture = Files.createTempFile("telumia-real-pointer-", ".avi")
        Files.write(fixture, originalAviFixture())
        Files.write(File(evidence, "fixture.avi").toPath(), originalAviFixture())
        val measured = CountDownLatch(1)
        val resolving = CountDownLatch(1)
        val ready = CountDownLatch(1)
        val secondReady = CountDownLatch(1)
        val enters = AtomicInteger()
        val resolutions = AtomicInteger()
        val resolutionsByCard = AtomicIntegerArray(2)
        val anchorBounds = AtomicReference<androidx.compose.ui.geometry.Rect>()
        val secondAnchorBounds = AtomicReference<androidx.compose.ui.geometry.Rect>()
        val rootWidth = AtomicInteger()
        val rootHeight = AtomicInteger()
        val failure = AtomicReference<String?>(null)
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        val robot = Robot().apply { autoDelay = 80 }
        val previousPointer = MouseInfo.getPointerInfo().location
        var frameCreated = false
        fun movePointer(point: Point) {
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500)
            var actual: Point
            do {
                robot.mouseMove(point.x, point.y)
                actual = MouseInfo.getPointerInfo().location
                record("OS move requested=$point actual=$actual")
                if (actual == point) break
                Thread.sleep(20)
            } while (System.nanoTime() < deadline)
            assertEquals(point, actual, "Robot must deliver the OS pointer to the requested screen coordinate")
        }
        fun waitFor(label: String, timeoutMs: Long = 4_000, condition: () -> Boolean) {
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
            while (!condition() && System.nanoTime() < deadline) Thread.sleep(25)
            assertTrue(condition(), "$label; nativeError=${failure.get()}")
        }
        fun capture(name: String, rectangle: Rectangle): BufferedImage =
            robot.createScreenCapture(rectangle).also { ImageIO.write(it, "png", File(evidence, name)) }
        try {
            movePointer(Point(20, 20))
            SwingUtilities.invokeAndWait {
                PosterZoomOverlayCoordinator.hide()
                panel = ComposePanel()
                window = JFrame("Telumia real pointer and video gate")
                window.add(panel)
                window.setBounds(100, 100, 1200, 750)
                window.isAlwaysOnTop = true
                frameCreated = true
                window.isVisible = true
                window.toFront()
                window.requestFocus()
                panel.setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                        Box(Modifier.fillMaxSize().onGloballyPositioned {
                            rootWidth.set(it.size.width)
                            rootHeight.set(it.size.height)
                        }, contentAlignment = Alignment.Center) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                for (cardIndex in 0..1) {
                                    HomePosterHoverPreview(
                                        item = MetaPreview("fixture:os-pointer:$cardIndex", "movie", "Real pointer / local AVI $cardIndex"),
                                        isWatched = false,
                                        onClick = {},
                                        onLongClick = null,
                                        previewSettings = PosterCardStyleUiState(
                                            hoverPreviewEnabled = true,
                                            hoverPreviewOpenDelayMillis = 500,
                                            hoverPreviewTrailerEnabled = true,
                                            hoverPreviewTrailerSoundEnabled = false,
                                            hoverPreviewTrailerStartSeconds = 0,
                                        ),
                                        trailerResolver = {
                                            resolutionsByCard.incrementAndGet(cardIndex)
                                            record("resolve=${resolutions.incrementAndGet()} pointer=${MouseInfo.getPointerInfo().location}")
                                            resolving.countDown()
                                            TrailerPlaybackSource(fixture.toAbsolutePath().toString())
                                        },
                                        onTrailerReady = {
                                            record("native-ready card=$cardIndex resolutions=${resolutions.get()}")
                                            if (cardIndex == 0) ready.countDown() else secondReady.countDown()
                                        },
                                        onTrailerError = { failure.set("native player error callback") },
                                    ) { hover ->
                                        Box(hover.size(200.dp, 300.dp).background(Color(0xFF18283A))
                                            .onPointerEvent(PointerEventType.Enter) { record("anchor-enter=${enters.incrementAndGet()}") }
                                            .onPointerEvent(PointerEventType.Exit) { record("anchor-exit pointer=${MouseInfo.getPointerInfo().location}") }
                                            .onGloballyPositioned {
                                                (if (cardIndex == 0) anchorBounds else secondAnchorBounds).set(it.boundsInRoot())
                                                measured.countDown()
                                            }) { Text("OS pointer target") }
                                    }
                                }
                            }
                            HomePosterHoverPreviewOverlayHost()
                        }
                    }
                }
            }
            assertTrue(measured.await(8, TimeUnit.SECONDS), "real anchor layout")
            SwingUtilities.invokeAndWait { window.toFront(); window.requestFocus() }
            robot.waitForIdle()
            Thread.sleep(700)
            assertNull(homePosterPreviewOwnership.active.value, "no preview may open with OS pointer outside the window")
            assertEquals(0, resolutions.get(), "outside pointer must not resolve trailers")
            lateinit var anchor: Rectangle
            lateinit var screen: Rectangle
            SwingUtilities.invokeAndWait {
                val offset = panel.locationOnScreen
                val bounds = anchorBounds.get()
                val sx = panel.width.toDouble() / rootWidth.get()
                val sy = panel.height.toDouble() / rootHeight.get()
                anchor = Rectangle(offset.x + (bounds.left * sx).toInt(), offset.y + (bounds.top * sy).toInt(),
                    (bounds.width * sx).toInt(), (bounds.height * sy).toInt())
                screen = Rectangle(window.locationOnScreen, window.size)
                record("windowActive=${window.isActive} transform=${window.graphicsConfiguration.defaultTransform} root=${rootWidth.get()}x${rootHeight.get()} panel=${panel.width}x${panel.height}")
            }
            capture("compose-before.png", screen)
            val anchorPoint = Point(anchor.x + anchor.width / 2, anchor.y + anchor.height / 2)
            movePointer(anchorPoint)
            record("anchor=$anchor requested=$anchorPoint actual=${MouseInfo.getPointerInfo().location}")
            waitFor("OS Robot must deliver Compose anchor Enter") { enters.get() > 0 }
            assertTrue(resolving.await(8, TimeUnit.SECONDS), "real OS hover must start resolver")
            assertTrue(ready.await(15, TimeUnit.SECONDS), "real overlay must report native ready; ${failure.get()}")
            lateinit var host: NativePlayerHost
            lateinit var videoBounds: Rectangle
            SwingUtilities.invokeAndWait {
                fun find(component: Component): NativePlayerHost? =
                    (component as? NativePlayerHost) ?: (component as? Container)?.components?.firstNotNullOfOrNull(::find)
                host = requireNotNull(find(panel)) { "native video host missing after ready" }
                videoBounds = Rectangle(host.locationOnScreen, host.size)
            }
            // Observe the real libmpv property without introducing production test hooks.
            val callback = requireNotNull(host.onPeerReady)
            val controllerField = callback.javaClass.declaredFields.first { it.type == NativePlayerController::class.java }
            controllerField.isAccessible = true
            val controller = controllerField.get(callback) as NativePlayerController
            val handleField = NativePlayerController::class.java.getDeclaredField("handle").apply { isAccessible = true }
            val handle = handleField.getLong(controller)
            assertTrue(handle != 0L, "real decoder handle before pointer transfer")
            record("native-before-transfer handle=$handle snapshot=${controller.snapshot()} paused=${NativePlayerBridge.isPaused(handle)} volume=${NativePlayerBridge.volume(handle)}")
            val overlayPoint = Point(videoBounds.x + 20, videoBounds.y + videoBounds.height / 2)
            assertFalse(anchor.contains(overlayPoint), "transfer target must be outside the original poster")
            movePointer(overlayPoint)
            repeat(20) {
                record("transfer-snapshot handle=${handleField.getLong(controller)} snapshot=${controller.snapshot()}")
                Thread.sleep(20)
            }
            capture("compose-transfer.png", screen)
            record("after-transfer point=$overlayPoint actual=${MouseInfo.getPointerInfo().location} resolutions=${resolutions.get()} owner=${homePosterPreviewOwnership.active.value != null}")
            assertTrue(homePosterPreviewOwnership.active.value != null, "pointer inside native overlay must retain ownership")
            assertEquals(1, resolutions.get(), "transfer must not restart trailer resolution")
            assertEquals(handle, handleField.getLong(controller), "transfer must retain the same native handle")
            assertEquals(0f, NativePlayerBridge.volume(handle), "inline native trailer volume must be zero")
            assertFalse(NativePlayerBridge.isPaused(handle), "visible native trailer must be playing")

            var video = capture("compose-video.png", videoBounds)
            fun fixturePixels(image: BufferedImage): Int {
                var count = 0
                for (y in 0 until image.height) for (x in 0 until image.width) {
                    val pixel = java.awt.Color(image.getRGB(x, y))
                    val channels = listOf(pixel.red, pixel.green, pixel.blue).sorted()
                    if (channels[2] > 220 && channels[1] < 40) count++
                }
                return count
            }
            waitFor("visible native pixels must match the red/green/blue local AVI", 2_000) {
                video = capture("compose-video.png", videoBounds)
                fixturePixels(video) > video.width * video.height / 2
            }
            capture("compose-after.png", screen)
            record("os-enters=${enters.get()} resolutions=${resolutions.get()} overlay=$overlayPoint video=$videoBounds fixturePixels=${fixturePixels(video)} totalPixels=${video.width * video.height} nativeVolume=${NativePlayerBridge.volume(handle)}")

            val firstOwner = homePosterPreviewOwnership.active.value
            lateinit var secondAnchor: Rectangle
            SwingUtilities.invokeAndWait {
                val offset = panel.locationOnScreen
                val bounds = secondAnchorBounds.get()
                val sx = panel.width.toDouble() / rootWidth.get()
                val sy = panel.height.toDouble() / rootHeight.get()
                secondAnchor = Rectangle(offset.x + (bounds.left * sx).toInt(), offset.y + (bounds.top * sy).toInt(),
                    (bounds.width * sx).toInt(), (bounds.height * sy).toInt())
            }
            movePointer(Point(secondAnchor.x + secondAnchor.width / 2, secondAnchor.y + secondAnchor.height / 2))
            assertTrue(secondReady.await(15, TimeUnit.SECONDS), "OS pointer transfer to second card must reach native readiness")
            waitFor("switching cards must release the first decoder") { handleField.getLong(controller) == 0L }
            val secondOwner = homePosterPreviewOwnership.active.value
            assertTrue(secondOwner != null && secondOwner !== firstOwner, "second card must own the exclusive preview slot")
            lateinit var secondHost: NativePlayerHost
            lateinit var secondVideoBounds: Rectangle
            SwingUtilities.invokeAndWait {
                fun find(component: Component): NativePlayerHost? =
                    (component as? NativePlayerHost) ?: (component as? Container)?.components?.firstNotNullOfOrNull(::find)
                secondHost = requireNotNull(find(panel))
                secondVideoBounds = Rectangle(secondHost.locationOnScreen, secondHost.size)
            }
            val secondCallback = requireNotNull(secondHost.onPeerReady)
            val secondControllerField = secondCallback.javaClass.declaredFields.first { it.type == NativePlayerController::class.java }
                .apply { isAccessible = true }
            val secondController = secondControllerField.get(secondCallback) as NativePlayerController
            val secondHandle = handleField.getLong(secondController)
            assertTrue(secondHandle != 0L, "second card's decoder must be live")
            val secondOverlayPoint = Point(secondVideoBounds.x + 20, secondVideoBounds.y + secondVideoBounds.height / 2)
            assertFalse(secondAnchor.contains(secondOverlayPoint), "second transfer target must be outside its poster")
            movePointer(secondOverlayPoint)
            Thread.sleep(600)
            assertEquals(secondHandle, handleField.getLong(secondController), "second video must retain the same handle")
            assertTrue(homePosterPreviewOwnership.active.value === secondOwner, "old teardown must not release the second preview")
            assertEquals(1, resolutionsByCard.get(0))
            assertEquals(1, resolutionsByCard.get(1))
            assertEquals(2, resolutions.get(), "each real card hover must resolve exactly once")
            assertEquals(0f, NativePlayerBridge.volume(secondHandle), "second native decoder must also be muted")
            assertFalse(NativePlayerBridge.isPaused(secondHandle))
            val secondVideo = capture("compose-second-video.png", secondVideoBounds)
            assertTrue(fixturePixels(secondVideo) > secondVideo.width * secondVideo.height / 2,
                "second card must show actual AVI pixels")
            capture("compose-second-card.png", screen)
            record("second-card ownerRetained=true perCardResolutions=1,1 volume=${NativePlayerBridge.volume(secondHandle)} fixturePixels=${fixturePixels(secondVideo)}")
            movePointer(Point(20, 20))
            waitFor("OS pointer exit must release preview ownership") { homePosterPreviewOwnership.active.value == null }
            waitFor("OS pointer exit must detach the decoder") { handleField.getLong(secondController) == 0L }
            capture("compose-teardown.png", screen)
            assertNull(homePosterPreviewOwnership.active.value)
            record("PASS: real pointer transfer, visible AVI frame, native volume zero, ownership and decoder teardown")
        } catch (error: Throwable) {
            record("FAIL: ${error.stackTraceToString()}")
            if (frameCreated) runCatching { capture("compose-failure.png", Rectangle(window.locationOnScreen, window.size)) }
            throw error
        } finally {
            if (frameCreated) SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); window.dispose() }
            robot.mouseMove(previousPointer.x, previousPointer.y)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (Files.exists(fixture) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(fixture) }
                if (Files.exists(fixture)) Thread.sleep(50)
            }
            assertFalse(Files.exists(fixture), "teardown must release the fixture file")
        }
    }
}
