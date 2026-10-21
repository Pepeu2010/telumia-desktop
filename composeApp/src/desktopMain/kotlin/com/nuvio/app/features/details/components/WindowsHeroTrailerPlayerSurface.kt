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
) {
    val host = remember { NativePlayerHost() }
    val controller = remember(host) {
        NativePlayerController(host = host, nativeControlsEnabled = false)
    }
    val latestOnReady = rememberUpdatedState(onReady)
    val latestOnEnded = rememberUpdatedState(onEnded)
    val latestOnError = rememberUpdatedState(onError)
    var hostReady by remember(host) { mutableStateOf(false) }
    var mediaReady by remember(controller) { mutableStateOf(false) }
    var terminalReported by remember(controller) { mutableStateOf(false) }

    DisposableEffect(host) {
        host.onFirstFullSizePaint = { hostReady = true }
        onDispose {
            host.onFirstFullSizePaint = null
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

        // These previews are silent by default. If the user explicitly enables sound and
        // the extractor returned a separate audio track, fall back to artwork instead of
        // playing a misleading, video-only preview on Windows.
        if (!muted && !sourceAudioUrl.isNullOrBlank()) {
            controller.pause()
            controller.dispose()
            mediaReady = false
            if (!terminalReported) {
                terminalReported = true
                latestOnError.value()
            }
            return@LaunchedEffect
        }

        terminalReported = false
        mediaReady = false
        controller.attach(
            sourceUrl = sourceUrl,
            sourceHeaders = TrailerExtractionPlatform.defaultHeaders,
            playWhenReady = false,
            initialPositionMs = startPositionMillis.coerceAtLeast(0L),
            decoderPriority = 0,
            nvidiaRtxSuperResolutionEnabled = false,
            onError = {
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
        if (playWhenReady) controller.play() else controller.pause()
    }

    LaunchedEffect(controller, mediaReady) {
        if (!mediaReady) return@LaunchedEffect
        while (true) {
            if (controller.snapshot().isEnded) {
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
