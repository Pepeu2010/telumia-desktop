package com.nuvio.app.features.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.core.ui.heroStretchHeight
import com.nuvio.app.features.home.MetaPreview

/** Reserves the hero height while giving the animated image a full-window layer. */
@Composable
internal fun HomeSelectedHero(
    items: List<MetaPreview>,
    highlighted: MetaPreview?,
    viewportHeight: Dp,
    mobileBelowSectionHeightHint: Dp?,
    sectionPadding: Dp?,
    listState: LazyListState?,
    stretchPx: () -> Float,
    onItemClick: ((MetaPreview) -> Unit)?,
    onBackgroundItemChanged: (MetaPreview) -> Unit = {},
) {
    val duration = LocalUiMotion.current.durationMillis(360)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val layout = homeHeroLayout(maxWidth.value, viewportHeight.value,
            mobileBelowSectionHeightHint?.value, preferDesktopLayout = true)
        Box(Modifier.fillMaxWidth().heroStretchHeight(layout.heroHeight, stretchPx)) {
            Crossfade(
                targetState = highlighted,
                animationSpec = tween(duration),
                modifier = Modifier.fillMaxSize()
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .requiredHeight(viewportHeight.coerceAtLeast(layout.heroHeight)),
                label = "HomeSelectedBackdrop",
            ) { selected ->
                Box(Modifier.fillMaxSize()) {
                    HomeHeroSection(
                        items = selected?.let(::listOf) ?: items,
                        viewportHeight = viewportHeight,
                        mobileBelowSectionHeightHint = mobileBelowSectionHeightHint,
                        sectionPadding = sectionPadding,
                        listState = listState,
                        stretchPx = stretchPx,
                        onItemClick = onItemClick,
                        onBackgroundItemChanged = onBackgroundItemChanged,
                    )
                }
            }
        }
    }
}
