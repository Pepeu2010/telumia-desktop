package com.nuvio.app.features.livetv

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.UUID

/** Small local configuration; guide payloads and credentials never enter profile sync. */
internal class LiveTvSourceStore(
    private val profileId: () -> Int = { ProfileRepository.activeProfileId },
    private val read: (Int) -> String? = {
        DesktopStorage.store("telumia_live_tv_sources").getString("sources_v1_$it")
    },
    private val write: (Int, String) -> Unit = { id, value ->
        DesktopStorage.store("telumia_live_tv_sources").putString("sources_v1_$id", value)
    },
) {
    private val lock = Any()
    private val json = Json { ignoreUnknownKeys = true }

    fun list(): List<LiveTvSource> = synchronized(lock) { decode(read(profileId())) }

    fun add(name: String, playlistUrl: String, epgUrl: String? = null): LiveTvSource {
        val source = LiveTvSource(
            UUID.randomUUID().toString(),
            name.trim().take(80).ifBlank { "Lista de canais" },
            requireHttpUrl(playlistUrl),
            epgUrl?.trim()?.takeIf { it.isNotEmpty() }?.let(::requireHttpUrl),
        )
        update { sources ->
            require(sources.size < 20) { "Limite de fontes de TV atingido" }
            sources + source
        }
        return source
    }

    fun setEnabled(id: String, enabled: Boolean) = update { sources ->
        sources.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }

    fun remove(id: String) = update { sources -> sources.filterNot { it.id == id } }

    private fun update(transform: (List<LiveTvSource>) -> List<LiveTvSource>) = synchronized(lock) {
        val owner = profileId()
        val next = json.encodeToString(transform(decode(read(owner))))
        require(next.length <= MAX_CONFIG_CHARS) { "Configuracao de TV excede o limite" }
        write(owner, next)
    }

    private fun decode(raw: String?): List<LiveTvSource> {
        if (raw.isNullOrBlank()) return emptyList()
        require(raw.length <= MAX_CONFIG_CHARS) { "Configuracao de TV excede o limite" }
        // Do not overwrite a corrupt configuration with an empty list during an edit.
        val sources = try { json.decodeFromString<List<LiveTvSource>>(raw) }
        catch (_: Exception) { error("Nao foi possivel ler as fontes de TV salvas") }
        require(sources.size <= 20 && sources.map { it.id }.distinct().size == sources.size) {
            "Configuracao de fontes de TV invalida"
        }
        return sources
    }

    private fun requireHttpUrl(raw: String): String {
        val value = raw.trim()
        val uri = runCatching { URI(value) }.getOrNull()
        require(value.length <= 4096 && uri != null &&
            uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null) {
            "Informe uma URL HTTP ou HTTPS valida sem credenciais embutidas"
        }
        return value
    }

    private companion object { const val MAX_CONFIG_CHARS = 64 * 1024 }
}
