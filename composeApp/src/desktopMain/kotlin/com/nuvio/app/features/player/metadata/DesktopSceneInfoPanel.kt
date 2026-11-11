package com.nuvio.app.features.player.metadata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.scene_info_actor
import nuvio.composeapp.generated.resources.scene_info_chapter
import nuvio.composeapp.generated.resources.scene_info_chapter_context
import nuvio.composeapp.generated.resources.scene_info_chapter_current
import nuvio.composeapp.generated.resources.scene_info_chapter_next
import nuvio.composeapp.generated.resources.scene_info_chapter_previous
import nuvio.composeapp.generated.resources.scene_info_close
import nuvio.composeapp.generated.resources.scene_info_empty
import nuvio.composeapp.generated.resources.scene_info_music
import nuvio.composeapp.generated.resources.scene_info_seek_chapter
import nuvio.composeapp.generated.resources.scene_info_title
import nuvio.composeapp.generated.resources.scene_info_trivia
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DesktopSceneInfoPanel(
    entries: List<SceneInfoEntry>,
    onDismiss: () -> Unit,
    onSeekToChapterStart: (Long) -> Unit = {},
) {
    val closeFocus = androidx.compose.runtime.remember { FocusRequester() }
    val chapterEntries = entries.filter { it.kind == TimedMetadataKind.CHAPTER && it.chapterRelation != null }
    val factEntries = entries.filterNot { it.kind == TimedMetadataKind.CHAPTER && it.chapterRelation != null }
    Surface(
        modifier = Modifier.fillMaxSize().testTag("scene-info-panel").onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                onDismiss()
                true
            } else false
        },
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.scene_info_title), Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRequester(closeFocus).testTag("scene-info-close")) {
                    Text(stringResource(Res.string.scene_info_close))
                }
            }
            if (entries.isEmpty()) {
                Text(stringResource(Res.string.scene_info_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (chapterEntries.isNotEmpty()) {
                        item(key = "chapter-context-heading") {
                            Text(
                                stringResource(Res.string.scene_info_chapter_context),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(chapterEntries, key = SceneInfoEntry::id) { entry ->
                            SceneInfoChapterRow(entry, onSeekToChapterStart)
                        }
                    }
                    items(factEntries, key = SceneInfoEntry::id) { entry ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.kind.label(), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 3,
                                overflow = TextOverflow.Ellipsis)
                            entry.detail?.takeIf(String::isNotBlank)?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { closeFocus.requestFocus() }
}

@Composable
private fun SceneInfoChapterRow(
    entry: SceneInfoEntry,
    onSeekToChapterStart: (Long) -> Unit,
) {
    val relation = requireNotNull(entry.chapterRelation)
    val startMs = entry.startMs
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                when (relation) {
                    SceneInfoChapterRelation.PREVIOUS -> stringResource(Res.string.scene_info_chapter_previous)
                    SceneInfoChapterRelation.CURRENT -> stringResource(Res.string.scene_info_chapter_current)
                    SceneInfoChapterRelation.NEXT -> stringResource(Res.string.scene_info_chapter_next)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(entry.title, style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            startMs?.let {
                Text(formatSceneInfoTimestamp(it), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(
            onClick = { startMs?.takeIf { it >= 0L }?.let(onSeekToChapterStart) },
            enabled = startMs != null && startMs >= 0L,
            modifier = Modifier.testTag("scene-info-chapter-seek-${relation.name.lowercase()}"),
        ) {
            Text(stringResource(Res.string.scene_info_seek_chapter))
        }
    }
}

private fun formatSceneInfoTimestamp(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3_600L
    fun pad2(value: Long) = if (value < 10L) "0$value" else value.toString()
    return if (hours > 0L) "$hours:${pad2(minutes)}:${pad2(seconds)}" else "${totalSeconds / 60L}:${pad2(seconds)}"
}

@Composable
private fun TimedMetadataKind.label(): String = when (this) {
    TimedMetadataKind.ACTOR -> stringResource(Res.string.scene_info_actor)
    TimedMetadataKind.MUSIC -> stringResource(Res.string.scene_info_music)
    TimedMetadataKind.CHAPTER -> stringResource(Res.string.scene_info_chapter)
    TimedMetadataKind.TRIVIA -> stringResource(Res.string.scene_info_trivia)
    else -> stringResource(Res.string.scene_info_title)
}
