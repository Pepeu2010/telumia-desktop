package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.originalAviFixture

class WindowsHeroTrailerPlayerSurfaceTest {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun composedInlineSurfaceReportsReadyAndPlaybackEndForLocalVideo() {
        assumeTrue("Windows native inline preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val fixture = Files.createTempFile("telumia-inline-surface-", ".avi")
        Files.write(fixture, originalAviFixture())
        val ready = CountDownLatch(1)
        val ended = CountDownLatch(1)
        val failure = AtomicReference<String?>(null)
        lateinit var panel: ComposePanel
        lateinit var window: JFrame

        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                window = JFrame("Telumia inline preview integration")
                window.contentPane.add(panel)
                window.setSize(640, 360)
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                window.setLocation(bounds.x + bounds.width + 500, bounds.y + bounds.height + 500)
                window.isVisible = true
                panel.setContent {
                    Box(Modifier.fillMaxSize()) {
                        WindowsHeroTrailerPlayerSurface(
                            sourceUrl = fixture.toAbsolutePath().toString(),
                            sourceAudioUrl = null,
                            playWhenReady = true,
                            muted = true,
                            startPositionMillis = 0,
                            fillFrame = false,
                            modifier = Modifier.fillMaxSize(),
                            onReady = { ready.countDown() },
                            onEnded = { ended.countDown() },
                            onError = { failure.compareAndSet(null, "preview surface reported a playback error") },
                        )
                    }
                }
            }

            assertTrue(ready.await(15, TimeUnit.SECONDS), "the composed preview surface must reach media-ready")
            assertFalse(failure.get() != null, failure.get() ?: "")
            assertTrue(ended.await(12, TimeUnit.SECONDS), "the composed preview surface must report playback end")
            assertFalse(failure.get() != null, failure.get() ?: "")
        } finally {
            SwingUtilities.invokeAndWait {
                panel.setContent {}
                panel.dispose()
                window.dispose()
            }
            val deleteDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (Files.exists(fixture) && System.nanoTime() < deleteDeadline) {
                runCatching { Files.deleteIfExists(fixture) }
                if (Files.exists(fixture)) Thread.sleep(50)
            }
            assertFalse(Files.exists(fixture), "test fixture must be removed after native teardown")
        }
    }
}
