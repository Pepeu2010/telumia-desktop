package com.nuvio.app.features.player.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.player.PlatformPlayerSurface
import com.nuvio.app.features.player.PlayerControlsState
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

/** Uses the actual native player and controls bridge with an original local fixture. */
class NativePlayerSurfacePlaybackTest {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun realPlayerAdvancesWithControlsConfigured() {
        assumeTrue("Windows desktop integration", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val video = Files.createTempFile("telumia-player-visual-", ".mkv")
        Files.write(video, originalChapterMatroska(withAudio = true))
        val playing = CountDownLatch(1)
        val failure = AtomicReference<String?>(null)
        lateinit var frame: JFrame
        lateinit var panel: ComposePanel
        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                frame = JFrame("Telumia isolated player validation")
                frame.contentPane.add(panel)
                frame.setSize(800, 600)
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                frame.setLocation(bounds.x + bounds.width + 500, bounds.y + bounds.height + 500)
                frame.isVisible = true
                panel.setContent {
                    NuvioTheme {
                        PlatformPlayerSurface(
                            sourceUrl = video.toString(), modifier = Modifier.fillMaxSize(),
                            playerControlsState = PlayerControlsState(title = "Teste Telumia", controlsVisible = true),
                            onControllerReady = {},
                            onSnapshot = { if (!it.isLoading && it.isPlaying && it.positionMs > 200) playing.countDown() },
                            onError = { if (it != null) failure.compareAndSet(null, it) },
                        )
                    }
                }
            }
            assertTrue(playing.await(30, TimeUnit.SECONDS), failure.get() ?: "Native player did not advance")
        } finally {
            SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); frame.dispose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (Files.exists(video) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(video) }
                if (Files.exists(video)) Thread.sleep(50)
            }
        }
        assertTrue(!Files.exists(video), "Local fixture must be released")
    }
}
