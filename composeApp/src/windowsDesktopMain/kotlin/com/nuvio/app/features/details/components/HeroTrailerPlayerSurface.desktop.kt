package com.nuvio.app.features.details.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun HeroTrailerPlayerSurface(
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
    WindowsHeroTrailerPlayerSurface(
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
