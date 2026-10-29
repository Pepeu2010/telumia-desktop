package com.nuvio.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.LocalScreenActive
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.rememberUiMotionLoopFloat
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.settings.AppBrandWordmark
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_brand_name
import org.jetbrains.compose.resources.stringResource

/** Shared cinematic opening used while the real app startup/profile work is in progress. */
@Composable
internal fun TelumiaOpeningMotion(modifier: Modifier = Modifier) {
    val motion = LocalUiMotion.current
    val active = LocalScreenActive.current
    val palette = MaterialTheme.themePalette
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationMillis(820),
            easing = FastOutSlowInEasing,
        ),
        label = "telumiaOpeningReveal",
    )
    val atmosphere by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationMillis(1_360),
            easing = FastOutSlowInEasing,
        ),
        label = "telumiaOpeningAtmosphere",
    )
    val loadingSweep = rememberUiMotionLoopFloat(
        active = active,
        initialValue = 0f,
        targetValue = 1f,
        durationMillis = 1_650,
        label = "telumiaOpeningLoadingSweep",
    )

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds(),
    ) {
        OpeningAtmosphere(
            phase = atmosphere,
            primary = palette.secondary,
            secondary = palette.secondaryVariant,
            modifier = Modifier.fillMaxSize(),
        )

        val logoHeight = (maxHeight * 0.12f).coerceIn(60.dp, 108.dp)
        val sweepPosition = if (active && motion.allowsSpatialEffects) loadingSweep.value else 0.5f
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppBrandWordmark(
                contentDescription = stringResource(Res.string.app_brand_name),
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(logoHeight)
                    .graphicsLayer {
                        alpha = reveal
                        val scale = 0.94f + reveal * 0.06f
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - reveal) * 18.dp.toPx()
                    },
            )
            Spacer(Modifier.height(30.dp))
            OpeningLoadingSweep(
                progress = sweepPosition,
                accent = palette.secondary,
                modifier = Modifier.width(86.dp).height(2.dp),
            )
        }
    }
}

@Composable
private fun OpeningAtmosphere(
    phase: Float,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val maxDimension = maxOf(size.width, size.height)
        val drift = (phase - 0.5f) * size.width * 0.035f

        drawRect(color = Color.Black.copy(alpha = 0.32f), size = size)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * (0.49f + (phase - 0.5f) * 0.08f), size.height * 0.48f),
                radius = maxDimension * 0.44f,
            ),
            size = size,
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.82f, size.height * 0.56f),
                radius = maxDimension * 0.38f,
            ),
            size = size,
        )

        val lightRibbon = Path().apply {
            moveTo(-size.width * 0.08f + drift, size.height * 0.70f)
            cubicTo(
                size.width * 0.20f + drift,
                size.height * 0.43f,
                size.width * 0.65f + drift,
                size.height * 0.76f,
                size.width * 1.08f + drift,
                size.height * 0.47f,
            )
        }
        val ribbonBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                primary.copy(alpha = 0.20f),
                secondary.copy(alpha = 0.18f),
                Color.Transparent,
            ),
            startX = -size.width * 0.12f + drift,
            endX = size.width * 1.08f + drift,
        )
        drawPath(
            path = lightRibbon,
            brush = ribbonBrush,
            alpha = 0.40f,
            style = Stroke(width = maxDimension * 0.055f, cap = StrokeCap.Round),
        )
        drawPath(
            path = lightRibbon,
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                startX = -size.width * 0.12f + drift,
                endX = size.width * 1.08f + drift,
            ),
            alpha = 0.30f,
            style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round),
        )
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.56f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.72f),
                ),
                center = Offset(size.width * 0.5f, size.height * 0.48f),
                radius = maxDimension * 0.76f,
            ),
            size = size,
        )
    }
}

@Composable
private fun OpeningLoadingSweep(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(
            color = accent.copy(alpha = 0.22f),
            cornerRadius = radius,
        )

        val segmentWidth = size.width * 0.30f
        val left = (size.width + segmentWidth) * progress.coerceIn(0f, 1f) - segmentWidth
        drawRoundRect(
            color = accent.copy(alpha = 0.24f),
            topLeft = Offset(left - segmentWidth * 0.18f, -size.height * 0.45f),
            size = Size(segmentWidth * 1.36f, size.height * 1.9f),
            cornerRadius = CornerRadius(size.height),
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.68f),
                    Color.White.copy(alpha = 0.92f),
                    accent.copy(alpha = 0.68f),
                    Color.Transparent,
                ),
                startX = left,
                endX = left + segmentWidth,
            ),
            topLeft = Offset(left, 0f),
            size = Size(segmentWidth, size.height),
            cornerRadius = radius,
        )
    }
}
