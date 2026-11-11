package com.nuvio.app.features.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.components.HeroTrailerPlayerSurface
import com.nuvio.app.features.details.selectHeroTrailer
import com.nuvio.app.features.details.youtubePlaybackUrl
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.trailer.TrailerPlaybackResolver
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import com.nuvio.app.core.ui.LocalUiMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val HoverTrailerFadeDurationMillis = 480

@Composable
internal fun HomePosterHoverTrailer(
    playbackSource: TrailerPlaybackSource?,
    soundEnabled: Boolean,
    startPositionSeconds: Int,
    onReady: () -> Unit = {},
    onEnded: () -> Unit = {},
    onError: () -> Unit = {},
    onUserInteraction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val motion = LocalUiMotion.current
    val latestOnReady = rememberUpdatedState(onReady)
    val latestOnEnded = rememberUpdatedState(onEnded)
    val latestOnError = rememberUpdatedState(onError)
    var activeSource by remember(playbackSource) { mutableStateOf(playbackSource) }
    var fallbackAttempted by remember(playbackSource) { mutableStateOf(false) }
    var trailerReady by remember(playbackSource) { mutableStateOf(false) }
    var trailerFinished by remember(playbackSource) { mutableStateOf(false) }
    var playerMounted by remember(playbackSource) {
        mutableStateOf(playbackSource != null)
    }
    val trailerAlpha by animateFloatAsState(
        targetValue = if (trailerReady && !trailerFinished) 1f else 0f,
        animationSpec = tween(durationMillis = motion.durationMillis(HoverTrailerFadeDurationMillis)),
        label = "poster_hover_trailer_alpha",
    )

    LaunchedEffect(trailerFinished, playbackSource, motion) {
        if (trailerFinished && playbackSource != null) {
            delay(motion.durationMillis(HoverTrailerFadeDurationMillis).toLong())
            playerMounted = false
        }
    }

    activeSource?.takeIf { playerMounted }?.let { source ->
        HeroTrailerPlayerSurface(
            sourceUrl = source.videoUrl,
            sourceAudioUrl = source.audioUrl,
            playWhenReady = !trailerFinished,
            muted = !soundEnabled,
            startPositionMillis = startPositionSeconds.toLong() * 1_000L,
            fillFrame = true,
            modifier = modifier.graphicsLayer { alpha = trailerAlpha },
            onReady = {
                if (activeSource == source && !trailerFinished) {
                    trailerReady = true
                    latestOnReady.value()
                }
            },
            onEnded = {
                if (activeSource == source) {
                    trailerReady = false
                    trailerFinished = true
                    latestOnEnded.value()
                }
            },
            onError = {
                if (activeSource == source) {
                    trailerReady = false
                    val fallback = playbackSource?.fallbackVideoUrl
                        ?.takeIf { !fallbackAttempted && it.isNotBlank() && it != source.videoUrl }
                    if (fallback != null) {
                        fallbackAttempted = true
                        activeSource = TrailerPlaybackSource(videoUrl = fallback)
                    } else {
                        trailerFinished = true
                        latestOnError.value()
                    }
                }
            },
            onUserInteraction = onUserInteraction,
        )
    }
}

internal suspend fun resolveHomePosterHoverTrailerPlaybackSource(
    item: MetaPreview,
): TrailerPlaybackSource? {
    val meta = MetaDetailsRepository.peek(type = item.type, id = item.id)
        ?: MetaDetailsRepository.fetch(type = item.type, id = item.id)
    val trailer = selectHeroTrailer(meta?.trailers.orEmpty())
        ?: return null
    return try {
        TrailerPlaybackResolver.resolveFromYouTubeUrl(trailer.youtubePlaybackUrl())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }
}
