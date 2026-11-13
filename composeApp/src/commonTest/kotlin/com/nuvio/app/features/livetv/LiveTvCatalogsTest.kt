package com.nuvio.app.features.livetv

import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.home.MetaPreview
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveTvCatalogsTest {
    @Test
    fun fullCatalogPagePreservesChannelsBeyondTheOldHomePreviewLimit() {
        val payload = (1..85).joinToString(prefix = "{\"metas\":[", postfix = "]}") {
            "{\"id\":\"channel:$it\",\"type\":\"tv\",\"name\":\"Canal $it\"}"
        }
        val parsed = com.nuvio.app.features.home.HomeCatalogParser.parseCatalogResponse(payload)
        val channels = projectLiveTvChannels(listOf(
            LiveTvCatalogResponse(request("tv", "TV source", "channels"), parsed.items),
        ))
        assertEquals(85, channels.size)
        assertEquals("Canal 85", channels.last().meta.name)
    }

    @Test
    fun installedCatalogsIncludeOnlyEnabledTvAndChannelCatalogs() {
        val enabled = addon(
            url = "https://tv.example/manifest.json",
            name = "TV source",
            catalogs = listOf(
                AddonCatalog(type = "movie", id = "popular", name = "Popular"),
                AddonCatalog(type = "TV", id = "channels", name = "Channels"),
                AddonCatalog(type = " channel ", id = "local", name = "Local"),
            ),
        )
        val disabled = addon(
            url = "https://off.example/manifest.json",
            name = "Disabled source",
            catalogs = listOf(AddonCatalog(type = "tv", id = "all", name = "All")),
            enabled = false,
        )

        val requests = installedLiveTvCatalogs(listOf(enabled, disabled))

        assertEquals(listOf("channels", "local"), requests.map { it.catalog.id })
        assertEquals(listOf("TV source", "TV source"), requests.map { it.addonName })
    }

    @Test
    fun projectionDeduplicatesChannelIdsAndKeepsAvailableAddonSources() {
        val tvRequest = request(type = "tv", addon = "Frostview TV", catalogId = "channels")
        val channelRequest = request(type = "channel", addon = "Bestcine", catalogId = "live")
        val channels = projectLiveTvChannels(
            listOf(
                LiveTvCatalogResponse(
                    tvRequest,
                    listOf(MetaPreview(id = "br:news", type = "tv", name = "Notícias", logo = "news.png")),
                ),
                LiveTvCatalogResponse(
                    tvRequest,
                    listOf(MetaPreview(id = "br:news", type = "tv", name = "Notícias")),
                ),
                LiveTvCatalogResponse(
                    channelRequest,
                    listOf(MetaPreview(id = "sport:1", type = "channel", name = "Esporte")),
                ),
            ),
        )

        assertEquals(listOf(
            "${tvRequest.manifestUrl}|tv:br:news",
            "${channelRequest.manifestUrl}|channel:sport:1",
        ), channels.map { it.key })
        assertEquals(listOf("Frostview TV"), channels.first().sourceNames)
        assertEquals("channel", channels.last().streamType)
    }

    @Test
    fun sameChannelIdFromDifferentAddonsRemainsSelectableSeparately() {
        val first = request("tv", "First", "all")
        val second = request("tv", "Second", "all")
        val channels = projectLiveTvChannels(listOf(
            LiveTvCatalogResponse(first, listOf(MetaPreview("id:1", "tv", "News", genres = listOf("News")))),
            LiveTvCatalogResponse(second, listOf(MetaPreview("id:1", "tv", "News", genres = listOf("Sports")))),
        ))
        assertEquals(2, channels.size)
        assertEquals(listOf(LiveTvChannelCategory.NEWS, LiveTvChannelCategory.SPORTS), channels.map { it.category })
    }

    private fun request(type: String, addon: String, catalogId: String) = LiveTvCatalogRequest(
        manifestUrl = "https://${addon.lowercase().replace(' ', '-')}.example/manifest.json",
        addonName = addon,
        catalog = AddonCatalog(type = type, id = catalogId, name = catalogId),
    )

    private fun addon(
        url: String,
        name: String,
        catalogs: List<AddonCatalog>,
        enabled: Boolean = true,
    ) = ManagedAddon(
        manifestUrl = url,
        enabled = enabled,
        manifest = AddonManifest(
            id = name.lowercase().replace(' ', '.'),
            name = name,
            description = "",
            version = "1.0.0",
            resources = emptyList(),
            types = emptyList(),
            catalogs = catalogs,
            transportUrl = url,
        ),
    )
}
