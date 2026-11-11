package com.nuvio.app.features.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.originalChapterMatroska
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

class HomePosterTrailerFallbackTest {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun failedSeparateAudioRetriesCombinedSourceWithoutReportingTerminalError() {
        assumeTrue("Windows native preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val video = Files.createTempFile("telumia-preview-primary-", ".mkv")
        val fallback = Files.createTempFile("telumia-preview-fallback-", ".mkv")
        val bytes = originalChapterMatroska()
        Files.write(video, bytes)
        Files.write(fallback, originalChapterMatroska(withAudio = true))
        val missingAudio = video.resolveSibling(video.fileName.toString() + ".missing.wav")
        val ready = CountDownLatch(1)
        val ended = CountDownLatch(1)
        val failed = AtomicBoolean(false)
        val frameCreated = AtomicBoolean(false)
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                window = JFrame("Telumia trailer fallback integration")
                frameCreated.set(true)
                window.contentPane.add(panel)
                window.setSize(640, 360)
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                window.setLocation(bounds.x + bounds.width + 500, bounds.y + bounds.height + 500)
                window.isVisible = true
                panel.setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                        Box(Modifier.fillMaxSize()) {
                            HomePosterHoverTrailer(
                                playbackSource = TrailerPlaybackSource(video.toString(), missingAudio.toString(), fallback.toString()),
                                soundEnabled = true, startPositionSeconds = 0,
                                onReady = { ready.countDown() }, onEnded = { ended.countDown() },
                                onError = { failed.set(true) }, modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
            assertTrue(ready.await(40, TimeUnit.SECONDS), "the fallback preview must become ready")
            assertFalse(failed.get(), "primary failure must not be reported as terminal when fallback succeeds")
            assertTrue(ended.await(15, TimeUnit.SECONDS), "fallback preview must finish")
        } finally {
            if (frameCreated.get()) SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); window.dispose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while ((Files.exists(video) || Files.exists(fallback)) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(video); Files.deleteIfExists(fallback) }
                if (Files.exists(video) || Files.exists(fallback)) Thread.sleep(50)
            }
            assertFalse(Files.exists(video) || Files.exists(fallback), "preview fixtures must be released")
        }
    }
}
