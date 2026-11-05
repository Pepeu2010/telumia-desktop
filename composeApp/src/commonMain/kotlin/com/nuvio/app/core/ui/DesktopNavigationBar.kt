package com.nuvio.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.jelly.JellyNavigationBar
import com.nuvio.app.features.settings.NavBarStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@Composable
internal fun DesktopNavigationBar(
    items: List<FloatingNavigationItem>,
    modifier: Modifier = Modifier,
    scrollState: NuvioNavBarScrollState? = null,
    hazeState: HazeState? = null,
    contentPadding: PaddingValues = PaddingValues(top = 10.dp, bottom = 8.dp),
    navBarStyle: NavBarStyle = NavBarStyle.ADAPTIVE,
    isHeroEnabled: Boolean = false,
    profileSwitcherOpen: Boolean = false,
    windowWidth: Dp? = null,
    glowEnabled: Boolean = true,
) {
    if (items.isEmpty()) return
    val uiMotion = LocalUiMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val theme = MaterialTheme.nuvio
    val colors = theme.colors
    val space = NuvioTokens.Space
    val isHomeSelected = items.first().selected
    val isSettingsSelected = items.last().selected
    val scrollOffset = scrollState?.totalScrollOffset ?: 0f
    val isScrolledAwayFromTop = scrollOffset > if (isSettingsSelected) 70f else 35f
    val isFrosted = isHovered || profileSwitcherOpen ||
        ((isHomeSelected || isSettingsSelected) && isScrolledAwayFromTop)
    val labelFraction by animateFloatAsState(
        targetValue = if (uiMotion.allowsSpatialEffects) {
            desktopNavigationLabelFraction(navBarStyle, isHomeSelected, isHeroEnabled, isHovered, profileSwitcherOpen, isScrolledAwayFromTop)
        } else if (navBarStyle == NavBarStyle.COMPACT) 0f else 1f,
        animationSpec = tween(uiMotion.durationMillis(240), easing = FastOutSlowInEasing),
        label = "desktop_nav_labels",
    )
    val surfaceColor by animateColorAsState(
        targetValue = if (isFrosted) {
            colors.surfaceElevated.copy(alpha = 0.94f)
        } else {
            colors.background.copy(alpha = 0.76f)
        },
        animationSpec = tween(uiMotion.durationMillis(240), easing = FastOutSlowInEasing),
        label = "desktop_nav_surface",
    )
    val outlineColor by animateColorAsState(
        targetValue = colors.accent.copy(alpha = if (isFrosted) 0.24f else 0.12f),
        animationSpec = tween(uiMotion.durationMillis(240), easing = FastOutSlowInEasing),
        label = "desktop_nav_outline",
    )
    val sheenAlpha by animateFloatAsState(
        targetValue = if (isFrosted) 1f else 0f,
        animationSpec = tween(uiMotion.durationMillis(240), easing = FastOutSlowInEasing),
        label = "desktop_nav_sheen",
    )
    val glowStrength by animateFloatAsState(
        targetValue = if (glowEnabled) 1f else 0f,
        animationSpec = tween(uiMotion.durationMillis(400), easing = NuvioTokens.Motion.standard),
        label = "desktop_nav_glow",
    )
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().padding(contentPadding).padding(horizontal = space.s16),
        contentAlignment = Alignment.TopCenter,
    ) {
        val viewportWidth = windowWidth ?: maxWidth
        val iconSize = when {
            viewportWidth < 800.dp -> 26.dp
            viewportWidth > 1600.dp -> 30.dp
            else -> 28.dp
        }
        val trackWidth = ((iconSize + space.s20 + space.s64 * labelFraction) * items.size + space.s8).coerceAtMost(maxWidth)
        JellyNavigationBar(
            items = items,
            labelFraction = labelFraction,
            glowStrength = glowStrength,
            horizontalLabels = true,
            iconSize = iconSize,
            modifier = Modifier.width(trackWidth).height(iconSize + space.s20).hoverable(interactionSource),
        ) {
            Box(
                modifier = Modifier.matchParentSize()
                    .clip(theme.shapes.chip)
                    .then(
                        if (isFrosted && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) { blurRadius = 14.dp }
                        } else {
                            Modifier
                        },
                    )
                    .background(surfaceColor)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.textPrimary.copy(alpha = 0.14f * sheenAlpha),
                                colors.textPrimary.copy(alpha = 0.03f * sheenAlpha),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .border(theme.borders.thin, outlineColor, theme.shapes.chip),
            )
        }
    }
}

internal fun desktopNavigationLabelFraction(
    style: NavBarStyle,
    isHomeSelected: Boolean,
    isHeroEnabled: Boolean,
    isHovered: Boolean,
    profileSwitcherOpen: Boolean,
    isScrolledAwayFromTop: Boolean,
): Float = when (style) {
    NavBarStyle.EXPANDED -> 1f
    NavBarStyle.COMPACT -> 0f
    else -> when {
        isHovered || profileSwitcherOpen -> 1f
        isHomeSelected && isHeroEnabled -> 0f
        isHomeSelected && isScrolledAwayFromTop -> 0f
        else -> 1f
    }
}
