package com.nuvio.app.features.profiles

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.isDesktop
import coil3.compose.AsyncImage

@Composable
internal fun AvatarPicker(
    avatars: List<AvatarCatalogItem>,
    selectedAvatarId: String?,
    onAvatarSelected: (AvatarCatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (avatars.isEmpty()) return
    val spacing = 10.dp
    val minAvatarSize = 72.dp
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = ((maxWidth + spacing) / (minAvatarSize + spacing)).toInt().coerceAtLeast(1)
        Column(
            modifier = Modifier.focusGroup().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            avatars.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEach { avatar ->
                        AvatarChoiceItem(
                            avatar = avatar,
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            isSelected = avatar.id == selectedAvatarId,
                            onClick = { onAvatarSelected(avatar) },
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarChoiceItem(
    avatar: AvatarCatalogItem,
    modifier: Modifier,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val motion = LocalUiMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val highlighted = isSelected || isHovered || isFocused
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected || isFocused -> MaterialTheme.colorScheme.primary
            isHovered -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(motion.durationMillis(160)),
        label = "avatarChoiceBorder",
    )
    val choiceScale by animateFloatAsState(
        targetValue = when {
            isPressed -> motion.scale(0.96f)
            highlighted -> motion.scale(1.045f)
            else -> 1f
        },
        animationSpec = tween(motion.durationMillis(160)),
        label = "avatarChoiceScale",
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = choiceScale; scaleY = choiceScale }
            .clip(CircleShape)
            .background(
                avatar.bgColor?.let(::parseHexColor)
                    ?: MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (isSelected || isFocused) 3.dp else if (isHovered) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape,
            )
            .then(if (isDesktop) Modifier.hoverable(interactionSource) else Modifier)
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = avatarImageUrl(avatar),
            contentDescription = avatar.displayName,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}
