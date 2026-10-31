package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioAccentMarker

@Composable
fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeader) {
            DetailSectionTitle(title = title)
        }
        content()
    }
}

@Composable
fun DetailSectionTitle(
    title: String,
    fullWidth: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val titleModifier = if (fullWidth) modifier.fillMaxWidth() else modifier
    BoxWithConstraints(modifier = titleModifier) {
        val titleSize = if (maxWidth >= 720.dp) 22.sp else 20.sp
        Row(
            modifier = Modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NuvioAccentMarker(height = 22.dp)
            Text(
                text = title,
                modifier = Modifier.then(if (fullWidth) Modifier.weight(1f) else Modifier),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = titleSize,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
