package com.nuvio.app.features.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.originalAviFixture
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

/** Checks actual HWND composition; Compose semantics alone cannot detect video covering text. */
class HomeHeroNativePreviewTest {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun nativeHeroVideoLeavesTheTitleActionsAndAudioControlVisible() {
        assumeTrue(DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val fixture = Files.createTempFile("telumia-hero-bounds-", ".avi")
        Files.write(fixture, originalAviFixture())
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        val densityScale = AtomicReference(1f)
        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                window = JFrame("Telumia hero composition QA")
                window.contentPane.add(panel)
                window.setBounds(80, 80, 1000, 700)
                window.isAlwaysOnTop = true
                window.isVisible = true
                window.toFront()
                window.requestFocus()
                panel.setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                        val pager = rememberPagerState(pageCount = { 1 })
                        val scope = rememberCoroutineScope()
                        densityScale.set(LocalDensity.current.density)
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val layout = homeHeroLayout(maxWidth.value, maxHeight.value, preferDesktopLayout = true)
                            DesktopHomeHeroFrame(
                                items = listOf(MetaPreview("fixture:hero-bounds", "movie", "BRASIL EM CENA")),
                                pagerState = pager, listState = null, layout = layout,
                                heroWidthPx = 1000f, heroHeightPx = 700f, stretchPx = { 0f },
                                includePagerNeighbors = false, contentHorizontalPadding = 32.dp,
                                coroutineScope = scope, onItemClick = {},
                                trailerPlaybackSource = TrailerPlaybackSource(fixture.toString()),
                                trailerSoundEnabled = false, trailerStartPositionSeconds = 0,
                            )
                        }
                    }
                }
            }
            val robot = Robot()
            fun capture(): BufferedImage {
                var bounds = Rectangle()
                SwingUtilities.invokeAndWait { bounds = Rectangle(panel.locationOnScreen, panel.size) }
                return robot.createScreenCapture(bounds)
            }
            var frame = capture()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
            while (frame.nativeVideoPatchFraction() < 0.9f && System.nanoTime() < deadline) {
                Thread.sleep(150)
                frame = capture()
            }
            // The native renderer can present a paused first frame before Compose handles
            // onReady. Wait for the real UI repaint instead of sampling that intermediate frame.
            val controlsDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4)
            while (frame.audioControlPixels(densityScale.get()) <= 20 && System.nanoTime() < controlsDeadline) {
                Thread.sleep(100)
                frame = capture()
            }
            assertTrue(frame.nativeVideoPatchFraction() > 0.9f,
                "the original solid-color video must fill the expected native frame, not another window")
            val output = File("build/reports/telumia-hover-preview/windows-native-hero.png")
            output.parentFile.mkdirs()
            ImageIO.write(frame, "png", output)
            assertTrue(frame.videoSamples(0.02f, 0.48f) == 0, "native video must not cover title/actions")
            var titlePixels = 0
            for (x in 25 until (frame.width * 0.48f).toInt() step 2) {
                for (y in 150 until frame.height - 100 step 2) {
                    val c = java.awt.Color(frame.getRGB(x, y))
                    if (c.red > 215 && c.green > 215 && c.blue > 215) titlePixels++
                }
            }
            assertTrue(titlePixels > 40, "title/actions must remain painted beside the native video")
            assertTrue(frame.audioControlPixels(densityScale.get()) > 20,
                "the audio pill must remain painted above the native video frame")
        } finally {
            SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); window.dispose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (Files.exists(fixture) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(fixture) }
                if (Files.exists(fixture)) Thread.sleep(50)
            }
            assertFalse(Files.exists(fixture), "preview decoder must release its local media")
        }
    }
}

private fun BufferedImage.audioControlPixels(density: Float): Int {
    var matches = 0
    for (x in (width * 0.7f).toInt() until width - 25 step 2) {
        for (y in (98 * density).toInt() until (130 * density).toInt() step 2) {
            val c = java.awt.Color(getRGB(x, y))
            if (c.red > 215 && c.green > 215 && c.blue > 215) matches++
        }
    }
    return matches
}

private fun BufferedImage.nativeVideoPatchFraction(): Float {
    var matches = 0
    var samples = 0
    for (x in (width * 0.62f).toInt() until (width * 0.88f).toInt() step 4) {
        for (y in (height * 0.40f).toInt() until (height * 0.60f).toInt() step 4) {
            val c = java.awt.Color(getRGB(x, y))
            if ((c.red > 180 && c.green < 70 && c.blue < 70) ||
                (c.blue > 180 && c.red < 70 && c.green < 70)) matches++
            samples++
        }
    }
    return matches.toFloat() / samples.coerceAtLeast(1)
}

private fun BufferedImage.videoSamples(startFraction: Float, endFraction: Float): Int {
    var count = 0
    for (x in (width * startFraction).toInt() until (width * endFraction).toInt() step 4) {
        for (y in 0 until height step 4) {
            val c = java.awt.Color(getRGB(x, y))
            if ((c.red > 180 && c.green < 70 && c.blue < 70) ||
                (c.blue > 180 && c.red < 70 && c.green < 70)) count++
        }
    }
    return count
}
