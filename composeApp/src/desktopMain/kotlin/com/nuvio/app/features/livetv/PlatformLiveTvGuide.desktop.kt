package com.nuvio.app.features.livetv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.profiles.ProfileRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.live_tv_guide_failed
import nuvio.composeapp.generated.resources.live_tv_guide_missing
import nuvio.composeapp.generated.resources.live_tv_guide_updated
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun PlatformLiveTvGuide(onPlay: (LiveTvChannel) -> Unit) {
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val profileId = profileState.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId
    key(profileId, profileState.activeProfile?.id) {
        var open by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { open = true }) { Text("Fontes e programação") }
        if (open) {
            DesktopLiveTvGuideDialog(profileId, onDismiss = { open = false }, onPlay = {
                open = false
                onPlay(it)
            })
        }
    }
}

@Composable
private fun DesktopLiveTvGuideDialog(
    profileId: Int,
    onDismiss: () -> Unit,
    onPlay: (LiveTvChannel) -> Unit,
) {
    val store = remember(profileId) { LiveTvSourceStore(profileId = { profileId }) }
    val importer = remember { LiveTvSourceImporter() }
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf(emptyList<LiveTvSource>()) }
    var selected by remember { mutableStateOf<LiveTvSource?>(null) }
    var guide by remember { mutableStateOf<ImportedLiveTvSource?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var playlist by remember { mutableStateOf("") }
    var epg by remember { mutableStateOf("") }

    suspend fun readSources() = withContext(Dispatchers.IO) { store.list() }

    LaunchedEffect(store) {
        try { sources = readSources() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Não foi possível ler as fontes salvas." }
        finally { saving = false }
    }

    fun editSources(action: () -> Unit, afterSave: () -> Unit = {}) {
        if (saving) return
        saving = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                sources = readSources()
                afterSave()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Não foi possível salvar a fonte. Confira os endereços HTTP ou HTTPS."
            } finally {
                saving = false
            }
        }
    }

    LaunchedEffect(selected, reload) {
        val source = selected ?: return@LaunchedEffect
        loading = true
        error = null
        try { guide = importer.load(source) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Não foi possível carregar a lista de canais." }
        finally { loading = false }
    }

    AlertDialog(
        modifier = if (selected != null) Modifier.width(920.dp) else Modifier,
        onDismissRequest = onDismiss,
        title = { Text(selected?.name ?: "Fontes de TV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (selected == null) {
                    LazyColumn(Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(sources, key = LiveTvSource::id) { source ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    guide = null
                                    selected = source
                                }, enabled = source.enabled && !saving, modifier = Modifier.weight(1f)) {
                                    Text(source.name)
                                }
                                Switch(checked = source.enabled, enabled = !saving, onCheckedChange = {
                                    editSources({ store.setEnabled(source.id, it) })
                                })
                                TextButton(onClick = { editSources({ store.remove(source.id) }) }, enabled = !saving) {
                                    Text("Remover")
                                }
                            }
                        }
                    }
                    OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                    OutlinedTextField(playlist, { playlist = it }, label = { Text("Endereço M3U") }, singleLine = true)
                    OutlinedTextField(epg, { epg = it }, label = { Text("Endereço XMLTV (opcional)") }, singleLine = true)
                    Button(onClick = {
                        val fields = Triple(name, playlist, epg)
                        editSources({ store.add(fields.first, fields.second, fields.third) }) {
                            name = ""
                            playlist = ""
                            epg = ""
                        }
                    }, enabled = !saving && playlist.isNotBlank()) { Text("Adicionar fonte") }
                } else {
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    guide?.let { LiveTvImportedChannels(it, onPlay) }
                    TextButton(onClick = { reload++ }, enabled = !loading) { Text("Atualizar") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        dismissButton = if (selected != null) {{
            TextButton(onClick = { selected = null; guide = null; error = null }) { Text("Fontes") }
        }} else null,
    )
}

@Composable
private fun LiveTvImportedChannels(guide: ImportedLiveTvSource, onPlay: (LiveTvChannel) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val zone = remember { ZoneId.systemDefault() }
    val updatedAtFormatter = remember(zone) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(zone)
    }
    val formattedUpdateTime = remember(guide.guideUpdatedAtEpochMillis, updatedAtFormatter) {
        guide.guideUpdatedAtEpochMillis?.let { updatedAtFormatter.format(Instant.ofEpochMilli(it)) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val programsByChannel = remember(guide.programs) {
        guide.programs.sortedBy { it.startEpochMillis }.groupBy { it.sourceId to it.channelId }
    }
    when {
        guide.guideFailed -> Text(stringResource(Res.string.live_tv_guide_failed), color = MaterialTheme.colorScheme.error)
        formattedUpdateTime != null -> Text(
            stringResource(Res.string.live_tv_guide_updated, formattedUpdateTime, zone.id),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        !guide.guideConfigured -> Text(
            stringResource(Res.string.live_tv_guide_missing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (guide.channels.isEmpty()) Text("Nenhum canal válido nesta lista.")
    if (guide.channels.isNotEmpty() && guide.programs.isNotEmpty()) {
        var schedule by remember { mutableStateOf(true) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = schedule, onClick = { schedule = true }, label = { Text("Grade") })
            FilterChip(selected = !schedule, onClick = { schedule = false }, label = { Text("Agora e depois") })
        }
        if (schedule) {
            DesktopLiveTvSchedule(guide.channels, guide.programs, now, onPlay)
            return
        }
    }
    LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(guide.channels, key = LiveTvChannel::id) { channel ->
            val programs = channel.tvgId?.let { programsByChannel[channel.sourceId to it] }.orEmpty()
            val current = programs.firstOrNull { now >= it.startEpochMillis && now < it.endEpochMillis }
            val next = programs.firstOrNull { it.startEpochMillis >= (current?.endEpochMillis ?: now) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(channel.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(current?.let { "Agora: ${it.title}" } ?: "Sem programação atual")
                    next?.let { Text("Depois: ${it.title}", style = MaterialTheme.typography.bodySmall) }
                }
                TextButton(onClick = { onPlay(channel) }, enabled = channel.isPlayable) { Text("Assistir") }
            }
        }
    }
}
