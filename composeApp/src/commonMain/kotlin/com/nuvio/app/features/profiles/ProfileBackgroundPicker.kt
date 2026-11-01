package com.nuvio.app.features.profiles

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.isDesktop
import com.nuvio.app.features.membership.ProfileBackgroundCatalogItem
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.profile_background_custom
import nuvio.composeapp.generated.resources.profile_background_normal
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileBackgroundPicker(
    backgrounds: List<ProfileBackgroundCatalogItem>,
    selectedBackgroundId: String?,
    selectedBackgroundUrl: String?,
    customBackgroundUrl: String?,
    standardBackgroundColor: Color,
    onSelectionChange: (String?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().focusGroup().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProfileBackgroundItem(
            label = stringResource(Res.string.profile_background_normal),
            isSelected = selectedBackgroundId == null && selectedBackgroundUrl == null,
            onClick = { onSelectionChange(null, null) },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                lerp(MaterialTheme.colorScheme.surface, standardBackgroundColor, 0.3f),
                                lerp(MaterialTheme.colorScheme.background, standardBackgroundColor, 0.14f),
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            )
        }
        customBackgroundUrl?.trim()?.takeIf { it.isNotBlank() }?.let { imageUrl ->
            ProfileBackgroundItem(
                label = stringResource(Res.string.profile_background_custom),
                isSelected = selectedBackgroundId == null && selectedBackgroundUrl == imageUrl,
                onClick = { onSelectionChange(null, imageUrl) },
            ) {
                val context = LocalPlatformContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .memoryCacheKey("custom-profile-background-$imageUrl")
                        .diskCacheKey("custom-profile-background-$imageUrl")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        backgrounds.forEach { background ->
            ProfileBackgroundItem(
                label = background.displayName,
                isSelected = selectedBackgroundUrl == null && selectedBackgroundId == background.id,
                onClick = { onSelectionChange(background.id, null) },
            ) {
                AsyncImage(
                    model = background.landscapeImageUri ?: background.landscapeImageBytes,
                    contentDescription = background.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun ProfileBackgroundItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val palette = MaterialTheme.themePalette
    val motion = LocalUiMotion.current
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isEmphasized = isFocused || isHovered
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected || isFocused -> palette.secondary
            isHovered -> palette.secondaryVariant
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(motion.durationMillis(160)),
        label = "profileBackgroundBorder",
    )
    val emphasisScale by animateFloatAsState(
        targetValue = if (isEmphasized) motion.scale(1.035f) else 1f,
        animationSpec = tween(motion.durationMillis(160)),
        label = "profileBackgroundScale",
    )
    Column(
        modifier = Modifier
            .width(144.dp)
            .graphicsLayer { scaleX = emphasisScale; scaleY = emphasisScale }
            .clip(shape)
            .then(if (isDesktop) Modifier.hoverable(interactionSource) else Modifier)
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(81.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (isSelected || isFocused) 3.dp else if (isHovered) 2.dp else 1.dp,
                    color = borderColor,
                    shape = shape,
                ),
        ) {
            content()
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(24.dp)
                        .height(24.dp)
                        .clip(CircleShape)
                        .background(palette.accentBrush()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = palette.onSecondary,
                    )
                }
            }
        }
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
