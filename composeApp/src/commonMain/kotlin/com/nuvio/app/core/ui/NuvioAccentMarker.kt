package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NuvioAccentMarker(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp,
    width: Dp = 3.dp,
) {
    val palette = MaterialTheme.themePalette
    val colors = palette.accentGradient
    val brush = if (colors.size >= 2) Brush.verticalGradient(colors)
        else SolidColor(colors.firstOrNull() ?: palette.secondary)
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(brush),
    )
}
