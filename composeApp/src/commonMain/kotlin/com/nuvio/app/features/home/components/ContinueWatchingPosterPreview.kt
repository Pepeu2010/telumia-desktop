package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import com.nuvio.app.features.watchprogress.ContinueWatchingItem

/** Reuses the Home trailer lifecycle while keeping the original resume action. */
@Composable
internal fun ContinueWatchingPosterPreview(
    item: ContinueWatchingItem,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    previewSettings: PosterCardStyleUiState? = null,
    trailerPreviewSupported: Boolean = AppFeaturePolicy.hoverTrailerPlaybackSupported,
    trailerResolver: suspend (MetaPreview) -> TrailerPlaybackSource? = ::resolveHomePosterHoverTrailerPlaybackSource,
    content: @Composable (Modifier) -> Unit,
) {
    if (!enabled) {
        content(Modifier)
        return
    }
    val preview = remember(item) {
        MetaPreview(
            id = item.parentMetaId,
            type = item.parentMetaType,
            name = item.title,
            poster = item.poster ?: item.imageUrl,
            banner = item.background ?: item.imageUrl,
            logo = item.logo,
        )
    }
    HomePosterHoverPreview(
        item = preview,
        isWatched = item.isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
        previewSettings = previewSettings,
        trailerPreviewSupported = trailerPreviewSupported,
        trailerResolver = trailerResolver,
        content = content,
    )
}
