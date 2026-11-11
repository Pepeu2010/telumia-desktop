package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clipToBounds
import com.nuvio.app.features.player.PlayerResizeMode
import com.nuvio.app.features.player.desktop.NativePlayerController
import com.nuvio.app.features.player.desktop.NativePlayerHost
import com.nuvio.app.features.trailer.TrailerExtractionPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val TrailerReadyTimeoutMs = 12_000L
private const val TrailerSnapshotIntervalMs = 100L
private const val TrailerPlaybackStartThresholdMs = 500L

@Composable
internal fun WindowsHeroTrailerPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    playWhenReady: Boolean,
    muted: Boolean,
    startPositionMillis: Long,
    fillFrame: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    onUserInteraction: () -> Unit = {},
) {
    key(sourceUrl, sourceAudioUrl, startPositionMillis) {
        WindowsHeroTrailerPlayerSession(
            sourceUrl = sourceUrl,
            sourceAudioUrl = sourceAudioUrl,
            playWhenReady = playWhenReady,
            muted = muted,
            startPositionMillis = startPositionMillis,
            fillFrame = fillFrame,
            modifier = modifier,
            onReady = onReady,
            onEnded = onEnded,
            onError = onError,
            onUserInteraction = onUserInteraction,
        )
    }
}

@Composable
private fun WindowsHeroTrailerPlayerSession(
    sourceUrl: String,
    sourceAudioUrl: String?,
    playWhenReady: Boolean,
    muted: Boolean,
    startPositionMillis: Long,
    fillFrame: Boolean,
    modifier: Modifier,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    onUserInteraction: () -> Unit,
) {
    val host = remember { NativePlayerHost() }
    val controller = remember(host) {
        NativePlayerController(host = host, nativeControlsEnabled = false)
    }
    val latestOnReady = rememberUpdatedState(onReady)
    val latestOnEnded = rememberUpdatedState(onEnded)
    val latestOnError = rememberUpdatedState(onError)
    val latestOnUserInteraction = rememberUpdatedState(onUserInteraction)
    var hostReady by remember(host) { mutableStateOf(false) }
    var mediaReady by remember(controller) { mutableStateOf(false) }
    var terminalReported by remember(controller) { mutableStateOf(false) }

    DisposableEffect(host) {
        host.onFirstFullSizePaint = { hostReady = true }
        host.onCursorActivity = { latestOnUserInteraction.value() }
        onDispose {
            host.onFirstFullSizePaint = null
            host.onCursorActivity = null
            hostReady = false
        }
    }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }

    SwingPanel(
        factory = { host },
        modifier = modifier.fillMaxSize().clipToBounds(),
    )

    LaunchedEffect(controller, hostReady, sourceUrl, sourceAudioUrl, startPositionMillis, muted) {
        if (!hostReady || sourceUrl.isBlank()) return@LaunchedEffect

        terminalReported = false
        mediaReady = false
        controller.attach(
            sourceUrl = sourceUrl,
            sourceHeaders = TrailerExtractionPlatform.defaultHeaders,
            playWhenReady = false,
            initialPositionMs = startPositionMillis.coerceAtLeast(0L),
            // Prefer hardware for smooth previews, but retain software fallback
            // for trailers whose codec is unsupported by the device decoder.
            decoderPriority = 1,
            nvidiaRtxSuperResolutionEnabled = false,
            onError = {
                System.err.println("[TelumiaPreview] native player error detail=${it ?: "unknown"}")
                if (!terminalReported) {
                    terminalReported = true
                    latestOnError.value()
                }
            },
        )

        val ready = withTimeoutOrNull(TrailerReadyTimeoutMs) {
            while (true) {
                val snapshot = controller.snapshot()
                if (!snapshot.isLoading && snapshot.durationMs > 0L) break
                delay(TrailerSnapshotIntervalMs)
            }
            true
        } ?: false

        if (!ready) {
            System.err.println("[TelumiaPreview] media readiness timeout snapshot=${controller.snapshot()}")
            if (!terminalReported) {
                terminalReported = true
                controller.dispose()
                latestOnError.value()
            }
            return@LaunchedEffect
        }

        if (!muted && !sourceAudioUrl.isNullOrBlank()) {
            val existingTrackIds = controller.getAudioTracks().map { it.id }.toSet()
            val queued = controller.addExternalAudioAsync(sourceAudioUrl)
            val audioReady = queued && (withTimeoutOrNull(TrailerReadyTimeoutMs) {
                while (true) {
                    if (controller.getAudioTracks().any { it.id !in existingTrackIds && it.isSelected }) break
                    delay(TrailerSnapshotIntervalMs)
                }
                true
            } ?: false)
            if (!audioReady) {
                System.err.println("[TelumiaPreview] external audio unavailable")
                if (!terminalReported) {
                    terminalReported = true
                    controller.dispose()
                    latestOnError.value()
                }
                return@LaunchedEffect
            }
        }

        if (!muted && sourceAudioUrl.isNullOrBlank() && controller.getAudioTracks().none { it.isSelected }) {
            System.err.println("[TelumiaPreview] combined source has no selected audio")
            if (!terminalReported) {
                terminalReported = true
                controller.dispose()
                latestOnError.value()
            }
            return@LaunchedEffect
        }

        controller.setTransientResizeMode(if (fillFrame) PlayerResizeMode.Fill else PlayerResizeMode.Fit)
        controller.setMuted(muted)
        mediaReady = true
        latestOnReady.value()
    }

    LaunchedEffect(controller, mediaReady, playWhenReady, muted) {
        if (!mediaReady) return@LaunchedEffect
        controller.setMuted(muted)
        if (playWhenReady) {
            // Some native sources can report EOF while still paused at their first frame.
            // `play()` mirrors the regular player replay behavior and seeks to the requested
            // start point before unpausing when that happens.
            if (controller.snapshot().isEnded) {
                controller.seekTo(startPositionMillis.coerceAtLeast(0L))
            }
            controller.play()
        } else {
            controller.pause()
        }
    }

    LaunchedEffect(controller, mediaReady) {
        if (!mediaReady) return@LaunchedEffect
        var playbackAdvanced = false
        val playbackStartPositionMs = startPositionMillis.coerceAtLeast(0L)
        while (true) {
            val snapshot = controller.snapshot()
            // EOF can be true at load time before playback starts. Only report the trailer as
            // ended after observing real timeline movement beyond the initial frame.
            if (!playbackAdvanced && snapshot.positionMs >= playbackStartPositionMs + TrailerPlaybackStartThresholdMs) {
                playbackAdvanced = true
            }
            if (playbackAdvanced && snapshot.isEnded) {
                if (!terminalReported) {
                    terminalReported = true
                    latestOnEnded.value()
                }
                break
            }
            delay(TrailerSnapshotIntervalMs)
        }
    }
}
