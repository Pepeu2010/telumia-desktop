package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import com.nuvio.app.features.details.MetaDetails

@Composable
internal fun DesktopStripePlaceholder(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .background(
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to colorScheme.background,
                        0.38f to colorScheme.surface,
                        0.76f to colorScheme.surfaceVariant,
                        1f to colorScheme.background,
                    ),
                ),
            )
            .drawWithCache {
                val coolLight = Brush.radialGradient(
                    colors = listOf(colorScheme.secondary.copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(size.width * 0.72f, size.height * 0.28f),
                    radius = maxOf(size.width, size.height) * 0.64f,
                )
                val warmLight = Brush.radialGradient(
                    colors = listOf(colorScheme.primary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.46f, size.height * 0.86f),
                    radius = maxOf(size.width, size.height) * 0.46f,
                )
                val ray = colorScheme.onSurface.copy(alpha = 0.035f)
                onDrawBehind {
                    drawRect(coolLight)
                    drawRect(warmLight)
                    repeat(4) { index ->
                        val x = size.width * (0.46f + index * 0.075f)
                        drawLine(
                            color = ray,
                            start = Offset(x, 0f),
                            end = Offset(x + size.width * 0.20f, size.height),
                            strokeWidth = size.width * 0.018f,
                        )
                    }
                }
            },
    )
}

internal fun desktopYearLabel(meta: MetaDetails): String? =
    meta.releaseInfo
        ?.trim()
        ?.takeIf { it.length >= 4 }
        ?.take(4)

internal fun desktopSeasonCountLabel(meta: MetaDetails): String? {
    val seasons = meta.videos.mapNotNull { it.season }.filter { it > 0 }.toSet().size
    if (seasons <= 0) return null
    return if (seasons == 1) "1 Season" else "$seasons Seasons"
}
