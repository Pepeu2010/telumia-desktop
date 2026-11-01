package com.nuvio.app.features.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.core.ui.interactionEmphasisScale
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.isDesktop
import com.nuvio.app.isTelevisionDevice
import com.nuvio.app.navigation.LocalNativeNavigationBarHidden
import com.nuvio.app.navigation.LocalUseNativeNavigation

@Composable
internal fun PlayerBackControlButton(
    onClick: () -> Unit,
    contentDescription: String,
    buttonSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    if (LocalUseNativeNavigation.current && !LocalNativeNavigationBarHidden.current) return

    PlayerControlButton(onClick = onClick, modifier = modifier.size(buttonSize)) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
internal fun PlayerControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = LocalUiMotion.current
    val palette = MaterialTheme.themePalette
    val desktopHover = isDesktop && hovered
    val emphasized = focused || desktopHover

    val scale by animateFloatAsState(
        targetValue = if (pressed) {
            motion.scale(0.97f)
        } else {
            interactionEmphasisScale(
                isDesktop = isDesktop,
                isTelevision = isTelevisionDevice(),
                emphasized = emphasized,
                motionPolicy = motion,
            )
        },
        animationSpec = tween(
            durationMillis = motion.durationMillis(if (pressed) 100 else 160),
            easing = FastOutSlowInEasing,
        ),
        label = "playerControlScale",
    )
    val surfaceColor by animateColorAsState(
        targetValue = containerColor ?: when {
            focused -> palette.focusBackground
            desktopHover -> palette.secondary.copy(alpha = 0.16f)
            pressed -> palette.secondary.copy(alpha = 0.2f)
            else -> Color.Black.copy(alpha = 0.36f)
        },
        animationSpec = tween(motion.durationMillis(150)),
        label = "playerControlSurface",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            focused -> palette.focusRing
            desktopHover -> palette.secondary.copy(alpha = 0.78f)
            pressed -> palette.secondary.copy(alpha = 0.66f)
            else -> Color.White.copy(alpha = 0.18f)
        },
        animationSpec = tween(motion.durationMillis(150)),
        label = "playerControlBorder",
    )
    val foreground = contentColor ?: when {
        focused && containerColor == null -> palette.focusRing
        desktopHover && containerColor == null -> palette.secondary
        else -> Color.White
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (emphasized) 1f else 0f)
            .clip(CircleShape)
            .background(surfaceColor)
            .border(
                width = when {
                    focused -> 2.dp
                    desktopHover -> 1.5.dp
                    else -> 1.dp
                },
                color = borderColor,
                shape = CircleShape,
            )
            .then(if (isDesktop) Modifier.hoverable(interactionSource) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val buttonContentScope = this
        CompositionLocalProvider(LocalContentColor provides foreground) {
            buttonContentScope.content()
        }
    }
}
