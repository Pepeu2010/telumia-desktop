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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.jelly.JellyNavigationBar
import com.nuvio.app.features.settings.NavBarStyle
import com.nuvio.app.core.format.formatLocalTime
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay

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
    profileSwitcherContent: (@Composable () -> Unit)? = null,
    settingsSelected: Boolean? = null,
    windowWidth: Dp? = null,
    glowEnabled: Boolean = true,
    showClock: Boolean = false,
) {
    if (items.isEmpty()) return
    val uiMotion = LocalUiMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isProfileSwitcherFocused by remember { mutableStateOf(false) }
    val isHomeSelected = items.first().selected
    val isSettingsSelected = settingsSelected ?: items.last().selected
    val scrollOffset = scrollState?.totalScrollOffset ?: 0f
    var clockText by remember { mutableStateOf("") }
    LaunchedEffect(showClock) {
        if (!showClock) return@LaunchedEffect
        while (true) {
            clockText = formatLocalTime(kotlin.time.Clock.System.now().toEpochMilliseconds())
            delay(30_000L)
        }
    }
    val isScrolledAwayFromTop = scrollOffset > if (isSettingsSelected) 70f else 35f
    val isFrosted = isHovered || isProfileSwitcherFocused || profileSwitcherOpen ||
        ((isHomeSelected || isSettingsSelected) && isScrolledAwayFromTop)
    val labelFraction by animateFloatAsState(
        targetValue = desktopNavigationLabelFraction(
            navBarStyle,
            isHomeSelected,
            isHeroEnabled,
            isHovered || isProfileSwitcherFocused,
            profileSwitcherOpen,
            isScrolledAwayFromTop,
        ),
        animationSpec = tween(uiMotion.durationMillis(240), easing = FastOutSlowInEasing),
        label = "desktop_nav_labels",
    )
    val surfaceColor by animateColorAsState(
        targetValue = Color(0xB8080808),
        animationSpec = tween(uiMotion.durationMillis(240), easing = FastOutSlowInEasing),
        label = "desktop_nav_surface",
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
        modifier = modifier.fillMaxWidth().padding(contentPadding).padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val viewportWidth = windowWidth ?: maxWidth
        val profileOffset = if (viewportWidth >= 1600.dp) 72.dp else 0.dp
        val iconSize = when {
            viewportWidth < 800.dp -> 26.dp
            viewportWidth > 1600.dp -> 30.dp
            else -> 28.dp
        }
        val profileSlotWidth = if (profileSwitcherContent != null) iconSize + 20.dp else 0.dp
        val trackReservedWidth = if (profileSwitcherContent != null) profileSlotWidth + 16.dp else 0.dp
        // Keep the route capsule optically centered while leaving clear space for
        // the profile avatar at the left edge, as in the cinematic reference.
        val maxTrackWidth = (maxWidth - trackReservedWidth * 2).coerceAtLeast(0.dp)
        // Reserve the label space even while labels are faded out. Keeping this width
        // stable prevents a hover from moving tab hit targets under the pointer.
        val trackWidth = ((iconSize + 20.dp + 90.dp) * items.size + 8.dp)
            .coerceAtMost(maxTrackWidth)
        Box(Modifier.fillMaxWidth().height(iconSize + 20.dp)) {
            JellyNavigationBar(
                items = items,
                labelFraction = labelFraction,
                glowStrength = glowStrength,
                horizontalLabels = true,
                iconSize = iconSize,
                modifier = Modifier.align(Alignment.Center).width(trackWidth).height(iconSize + 20.dp)
                    .hoverable(interactionSource),
            ) {
                Box(
                    modifier = Modifier.matchParentSize()
                        .clip(RoundedCornerShape(50))
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
                                    Color.White.copy(alpha = 0.14f * sheenAlpha),
                                    Color.White.copy(alpha = 0.03f * sheenAlpha),
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50)),
                )
            }
            if (showClock && clockText.isNotBlank()) {
                Text(
                    text = clockText,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            profileSwitcherContent?.let { content ->
                val focusIndicator = if (isProfileSwitcherFocused) {
                    Modifier.border(2.dp, MaterialTheme.nuvio.colors.borderSelected, RoundedCornerShape(50))
                } else {
                    Modifier
                }
                Box(
                    modifier = Modifier.align(Alignment.CenterStart).width(profileSlotWidth).height(iconSize + 20.dp)
                        .offset(x = profileOffset)
                        .hoverable(interactionSource)
                        .onFocusChanged { isProfileSwitcherFocused = it.isFocused }
                        .then(focusIndicator),
                    contentAlignment = Alignment.Center,
                ) {
                    content()
                }
            }
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
    NavBarStyle.CLASSIC, NavBarStyle.COMPACT -> 0f
    // Cinematic home starts with a clean icon rail. Labels expand only on
    // pointer/focus interaction, so destinations remain discoverable on demand.
    NavBarStyle.ADAPTIVE -> if (isHovered || profileSwitcherOpen) 1f else 0f
}
