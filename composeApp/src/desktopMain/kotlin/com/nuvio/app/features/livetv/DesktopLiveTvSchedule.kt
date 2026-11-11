package com.nuvio.app.features.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.live_tv_timezone_label
import org.jetbrains.compose.resources.stringResource

private const val GuideSlotMillis = 30 * 60_000L
private const val GuideWindowMillis = 4 * GuideSlotMillis
private val GuideChannelWidth = 136.dp
private val GuideRowHeight = 56.dp

@Composable
internal fun DesktopLiveTvSchedule(
    channels: List<LiveTvChannel>,
    programs: List<LiveTvProgram>,
    now: Long,
    onPlay: (LiveTvChannel) -> Unit,
) {
    var offset by remember { mutableIntStateOf(0) }
    var selected by remember(programs) { mutableStateOf<LiveTvProgram?>(null) }
    val windowStart = now / GuideSlotMillis * GuideSlotMillis + offset * GuideWindowMillis
    val windowEnd = windowStart + GuideWindowMillis
    val zone = remember { ZoneId.systemDefault() }
    val clock = remember(zone) { DateTimeFormatter.ofPattern("HH:mm").withZone(zone) }
    val date = remember(zone) { DateTimeFormatter.ofPattern("dd/MM").withZone(zone) }
    val grouped = remember(programs) { programs.groupBy { it.sourceId to it.channelId } }
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${date.format(Instant.ofEpochMilli(windowStart))} · ${clock.format(Instant.ofEpochMilli(windowStart))}–${clock.format(Instant.ofEpochMilli(windowEnd))}",
                Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = { offset--; selected = null }, enabled = offset > -24) { Text("Anterior") }
            TextButton(onClick = { offset = 0; selected = null }, enabled = offset != 0) { Text("Agora") }
            TextButton(onClick = { offset++; selected = null }, enabled = offset < 24) { Text("Próximo") }
        }
        Text(
            stringResource(Res.string.live_tv_timezone_label, zone.id),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
        selected?.let { program ->
            Column(
                Modifier.fillMaxWidth().background(colors.surfaceContainerHigh, MaterialTheme.shapes.medium)
                    .padding(12.dp),
            ) {
                Text(program.title, style = MaterialTheme.typography.titleSmall)
                Text("${clock.format(Instant.ofEpochMilli(program.startEpochMillis))}–${clock.format(Instant.ofEpochMilli(program.endEpochMillis))}")
                program.description?.let { Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Canal", Modifier.width(GuideChannelWidth), style = MaterialTheme.typography.labelMedium)
            repeat(4) { slot ->
                Text(
                    clock.format(Instant.ofEpochMilli(windowStart + slot * GuideSlotMillis)),
                    Modifier.weight(1f), color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(channels, key = LiveTvChannel::id) { channel ->
                val channelPrograms = channel.tvgId?.let { grouped[channel.sourceId to it] }.orEmpty()
                val segments = remember(channelPrograms, channel.id, windowStart) {
                    channel.tvgId?.let { guideSegments(channelPrograms, channel.sourceId, it, windowStart, windowEnd) }.orEmpty()
                }
                Row(Modifier.fillMaxWidth().height(GuideRowHeight), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onPlay(channel) }, enabled = channel.isPlayable,
                        modifier = Modifier.width(GuideChannelWidth).heightIn(min = 48.dp)) {
                        Text(channel.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    BoxWithConstraints(
                        Modifier.weight(1f).fillMaxHeight().clip(MaterialTheme.shapes.small)
                            .background(colors.surfaceContainerLow),
                    ) {
                        val timelineWidth = maxWidth
                        if (segments.isEmpty()) {
                            Text("Sem programação", Modifier.align(Alignment.CenterStart).padding(horizontal = 10.dp),
                                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        for (segment in segments) {
                            key(segment.startMillis) {
                                val program = segment.program
                                val active = now in program.startEpochMillis until program.endEpochMillis
                                var focused by remember { mutableStateOf(false) }
                                val start = (segment.startMillis - windowStart).toFloat() / GuideWindowMillis
                                val duration = (segment.endMillis - segment.startMillis).toFloat() / GuideWindowMillis
                                val shape = MaterialTheme.shapes.small
                                Box(
                                    Modifier.offset(x = timelineWidth * start).width(timelineWidth * duration)
                                        .fillMaxHeight().padding(end = 2.dp).clip(shape)
                                        .background(if (active) colors.primaryContainer else colors.surfaceContainerHigh)
                                        .onFocusChanged { focused = it.isFocused }
                                        .then(if (focused || program == selected) Modifier.border(2.dp, colors.primary, shape) else Modifier)
                                        .clickable { selected = program }.padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(if (active) "Agora · ${program.title}" else program.title,
                                        color = if (active) colors.onPrimaryContainer else colors.onSurface,
                                        style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
