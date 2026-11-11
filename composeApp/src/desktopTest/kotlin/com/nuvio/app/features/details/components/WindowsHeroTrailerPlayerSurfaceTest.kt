package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import java.awt.GraphicsEnvironment
import java.nio.file.Files
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.originalChapterMatroska

class WindowsHeroTrailerPlayerSurfaceTest {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun unmutedCombinedSourceWithoutAudioReportsFailure() {
        assumeTrue("Windows native inline preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val video = Files.createTempFile("telumia-silent-combined-", ".mkv")
        Files.write(video, originalChapterMatroska())
        val failed = CountDownLatch(1)
        val ready = AtomicBoolean(false)
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                window = JFrame("Telumia combined audio integration")
                window.contentPane.add(panel)
                window.setSize(640, 360)
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                window.setLocation(bounds.x + bounds.width + 500, bounds.y + bounds.height + 500)
                window.isVisible = true
                panel.setContent {
                    WindowsHeroTrailerPlayerSurface(
                        sourceUrl = video.toString(), sourceAudioUrl = null,
                        playWhenReady = true, muted = false, startPositionMillis = 0,
                        fillFrame = false, modifier = Modifier.fillMaxSize(),
                        onReady = { ready.set(true) }, onEnded = {}, onError = { failed.countDown() },
                    )
                }
            }
            assertTrue(failed.await(30, TimeUnit.SECONDS), "video without audio must report an error")
            assertFalse(ready.get(), "video without audio must not report ready with sound")
        } finally {
            SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); window.dispose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (Files.exists(video) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(video) }
                if (Files.exists(video)) Thread.sleep(50)
            }
            assertFalse(Files.exists(video), "video fixture must be released")
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun missingSeparateAudioReportsFailureWithoutStartingSilentPreview() {
        assumeTrue("Windows native inline preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val video = Files.createTempFile("telumia-missing-audio-video-", ".mkv")
        Files.write(video, originalChapterMatroska())
        val missingAudio = video.resolveSibling(video.fileName.toString() + ".missing.wav")
        val failed = CountDownLatch(1)
        val ready = AtomicBoolean(false)
        val frameCreated = AtomicBoolean(false)
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                window = JFrame("Telumia missing audio integration")
                frameCreated.set(true)
                window.contentPane.add(panel)
                window.setSize(640, 360)
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                window.setLocation(bounds.x + bounds.width + 500, bounds.y + bounds.height + 500)
                window.isVisible = true
                panel.setContent {
                    Box(Modifier.fillMaxSize()) {
                        WindowsHeroTrailerPlayerSurface(
                            sourceUrl = video.toAbsolutePath().toString(),
                            sourceAudioUrl = missingAudio.toAbsolutePath().toString(),
                            playWhenReady = true, muted = false, startPositionMillis = 0,
                            fillFrame = false, modifier = Modifier.fillMaxSize(),
                            onReady = { ready.set(true) }, onEnded = {}, onError = { failed.countDown() },
                        )
                    }
                }
            }
            assertTrue(failed.await(30, TimeUnit.SECONDS), "missing audio must report an error")
            assertFalse(ready.get(), "a video-only preview must not report ready with sound")
        } finally {
            if (frameCreated.get()) SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); window.dispose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (Files.exists(video) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(video) }
                if (Files.exists(video)) Thread.sleep(50)
            }
            assertFalse(Files.exists(video), "video fixture must be released")
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun composedPreviewSelectsSeparateLocalAudioBeforeReportingReady() {
        assumeTrue("Windows native inline preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val video = Files.createTempFile("telumia-inline-video-", ".mkv")
        val audio = Files.createTempFile("telumia-inline-audio-", ".wav")
        Files.write(video, originalChapterMatroska())
        // Silent PCM avoids disturbing the desktop while exercising a real external audio track.
        val pcm = ByteArray(8_000 * 3 * 2)
        val wav = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcm.size); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1.toShort()); putShort(1.toShort()); putInt(8_000)
            putInt(16_000); putShort(2.toShort()); putShort(16.toShort())
            put("data".toByteArray()); putInt(pcm.size); put(pcm)
        }.array()
        Files.write(audio, wav)
        val ready = CountDownLatch(1)
        val ended = CountDownLatch(1)
        val failure = AtomicReference<String?>(null)
        val frameCreated = AtomicBoolean(false)
        lateinit var panel: ComposePanel
        lateinit var window: JFrame
        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel()
                window = JFrame("Telumia separate audio integration")
                frameCreated.set(true)
                window.contentPane.add(panel)
                window.setSize(640, 360)
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                window.setLocation(bounds.x + bounds.width + 500, bounds.y + bounds.height + 500)
                window.isVisible = true
                panel.setContent {
                    Box(Modifier.fillMaxSize()) {
                        WindowsHeroTrailerPlayerSurface(
                            sourceUrl = video.toAbsolutePath().toString(),
                            sourceAudioUrl = audio.toAbsolutePath().toString(),
                            playWhenReady = true, muted = false, startPositionMillis = 0,
                            fillFrame = false, modifier = Modifier.fillMaxSize(),
                            onReady = { ready.countDown() }, onEnded = { ended.countDown() },
                            onError = { failure.compareAndSet(null, "separate audio preview failed") },
                        )
                    }
                }
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), failure.get() ?: "external audio was not selected")
            assertFalse(failure.get() != null, failure.get() ?: "")
            assertTrue(ended.await(12, TimeUnit.SECONDS), "preview with external audio must finish")
            assertFalse(failure.get() != null, failure.get() ?: "")
        } finally {
            if (frameCreated.get()) SwingUtilities.invokeAndWait { panel.setContent {}; panel.dispose(); window.dispose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while ((Files.exists(video) || Files.exists(audio)) && System.nanoTime() < deadline) {
                runCatching { Files.deleteIfExists(video); Files.deleteIfExists(audio) }
                if (Files.exists(video) || Files.exists(audio)) Thread.sleep(50)
            }
            assertFalse(Files.exists(video) || Files.exists(audio), "preview fixtures must be released")
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun composedInlineSurfaceReportsReadyAndPlaybackEndForLocalVideo() {
        assumeTrue("Windows native inline preview integration gate", DesktopHostOs.current == DesktopHostOs.WINDOWS)
        val fixture = Files.createTempFile("telumia-inline-surface-", ".mkv")
        Files.write(fixture, originalChapterMatroska())
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
