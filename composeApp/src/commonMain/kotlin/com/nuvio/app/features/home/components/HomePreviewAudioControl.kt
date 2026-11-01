package com.nuvio.app.features.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.LocalUiMotion
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_preview_enable_sound
import nuvio.composeapp.generated.resources.home_preview_disable_sound
import nuvio.composeapp.generated.resources.home_preview_muted
import nuvio.composeapp.generated.resources.home_preview_with_sound
import org.jetbrains.compose.resources.stringResource

/** Appears only after the native preview is ready; sound is local to this card. */
@Composable
internal fun HomePreviewAudioControl(
    visible: Boolean,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalUiMotion.current
    val action = stringResource(if (soundEnabled) Res.string.home_preview_disable_sound else Res.string.home_preview_enable_sound)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(motion.durationMillis(180))),
        exit = fadeOut(tween(motion.durationMillis(120))),
    ) {
        Surface(
            onClick = onToggleSound,
            modifier = Modifier.testTag("home-preview-audio").semantics { contentDescription = action },
            shape = RoundedCornerShape(percent = 50),
            color = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        ) {
            Row(
                modifier = Modifier.heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(if (soundEnabled) Res.string.home_preview_with_sound else Res.string.home_preview_muted),
                    style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}
