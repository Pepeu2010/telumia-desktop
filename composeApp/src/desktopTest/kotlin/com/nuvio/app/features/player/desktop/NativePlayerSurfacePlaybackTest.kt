package com.nuvio.app.features.player.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.player.PlatformPlayerSurface
import com.nuvio.app.features.player.PlayerControlsState
import java.awt.GraphicsEnvironment
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    @Test fun separateTrailerAudioIsSelectedBeforePlayback() {
        assumeTrue("Windows desktop integration", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val video = Files.createTempFile("telumia-player-video-", ".mkv")
        val audio = Files.createTempFile("telumia-player-audio-", ".wav")
        Files.write(video, originalChapterMatroska())
        val pcm = ByteArray(8_000 * 3 * 2)
        Files.write(audio, ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcm.size); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1.toShort()); putShort(1.toShort()); putInt(8_000)
            putInt(16_000); putShort(2.toShort()); putShort(16.toShort())
            put("data".toByteArray()); putInt(pcm.size); put(pcm)
        }.array())
        val playingWithAudio = CountDownLatch(1)
        val failure = AtomicReference<String?>(null)
        val controller = AtomicReference<NativePlayerController?>()
        lateinit var frame: JFrame
        lateinit var panel: ComposePanel
        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                frame = JFrame("Telumia separate trailer audio validation")
                frame.contentPane.add(panel)
                frame.setSize(800, 600)
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                frame.setLocation(bounds.x + bounds.width + 500, bounds.y + bounds.height + 500)
                frame.isVisible = true
                panel.setContent {
                    NuvioTheme {
                        PlatformPlayerSurface(
                            sourceUrl = video.toString(), sourceAudioUrl = audio.toString(),
                            modifier = Modifier.fillMaxSize(),
                            onControllerReady = { controller.set(it as NativePlayerController) },
                            onSnapshot = { snapshot ->
                                if (snapshot.isPlaying && snapshot.positionMs > 200 &&
                                    controller.get()?.getAudioTracks()?.any { it.isSelected } == true) playingWithAudio.countDown()
                            },
                            onError = { if (it != null) failure.compareAndSet(null, it) },
                        )
                    }
                }
            }
            assertTrue(playingWithAudio.await(30, TimeUnit.SECONDS), failure.get() ?: "External audio was not selected")
        } finally {
            awaitNativeRelease(controller.get())
            SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); frame.dispose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while ((Files.exists(video) || Files.exists(audio)) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(video); Files.deleteIfExists(audio) }
                if (Files.exists(video) || Files.exists(audio)) Thread.sleep(50)
            }
        }
        assertTrue(!Files.exists(video) && !Files.exists(audio), "Local fixtures must be released")
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun realPlayerAdvancesWithControlsConfigured() {
        assumeTrue("Windows desktop integration", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val video = Files.createTempFile("telumia-player-visual-", ".mkv")
        Files.write(video, originalChapterMatroska(withAudio = true))
        val playing = CountDownLatch(1)
        val failure = AtomicReference<String?>(null)
        val controller = AtomicReference<NativePlayerController?>()
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
                            onControllerReady = { controller.set(it as NativePlayerController) },
                            onSnapshot = { if (!it.isLoading && it.isPlaying && it.positionMs > 200) playing.countDown() },
                            onError = { if (it != null) failure.compareAndSet(null, it) },
                        )
                    }
                }
            }
            assertTrue(playing.await(30, TimeUnit.SECONDS), failure.get() ?: "Native player did not advance")
        } finally {
            awaitNativeRelease(controller.get())
            SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); frame.dispose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (Files.exists(video) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(video) }
                if (Files.exists(video)) Thread.sleep(50)
            }
        }
        assertTrue(!Files.exists(video), "Local fixture must be released")
    }

    private fun awaitNativeRelease(controller: NativePlayerController?) {
        if (controller == null) return
        val released = CountDownLatch(1)
        val failure = AtomicReference<String?>(null)
        controller.releaseBeforeNavigation(
            onReleased = { released.countDown() },
            onReleaseFailed = { failure.set(it); released.countDown() },
        )
        assertTrue(released.await(12, TimeUnit.SECONDS), failure.get() ?: "Native player release timed out")
        assertTrue(failure.get() == null, failure.get() ?: "Native player release failed")
    }
}
