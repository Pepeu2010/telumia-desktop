package com.nuvio.app.features.livetv

import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.home.MetaPreview

internal data class LiveTvCatalogRequest(
    val manifestUrl: String,
    val addonName: String,
    val catalog: AddonCatalog,
)

internal data class LiveTvCatalogResponse(
    val request: LiveTvCatalogRequest,
    val items: List<MetaPreview>,
)

internal data class LiveTvChannelItem(
    val key: String,
    val meta: MetaPreview,
    val streamType: String,
    val sourceNames: List<String>,
)

internal fun installedLiveTvCatalogs(addons: List<ManagedAddon>): List<LiveTvCatalogRequest> =
    addons.asSequence()
        .filter { it.enabled && it.manifest != null }
        .flatMap { addon ->
            val manifest = addon.manifest ?: return@flatMap emptySequence()
            manifest.catalogs.asSequence()
                .filter { it.type.trim().lowercase() in LIVE_TV_CATALOG_TYPES }
                .map { catalog ->
                    LiveTvCatalogRequest(
                        manifestUrl = addon.manifestUrl,
                        addonName = addon.displayTitle.ifBlank { manifest.name },
                        catalog = catalog,
                    )
                }
        }
        .distinctBy { "${it.manifestUrl}|${it.catalog.type.lowercase()}|${it.catalog.id}" }
        .toList()

internal fun projectLiveTvChannels(responses: List<LiveTvCatalogResponse>): List<LiveTvChannelItem> {
    val grouped = linkedMapOf<String, Pair<MetaPreview, LinkedHashSet<String>>>()
    val streamTypes = linkedMapOf<String, String>()

    responses.forEach { response ->
        val streamType = response.request.catalog.type.trim().lowercase()
        response.items.forEach { meta ->
            if (meta.id.isBlank() || meta.name.isBlank()) return@forEach
            val key = "$streamType:${meta.id}"
            val entry = grouped[key]
            if (entry == null) {
                grouped[key] = meta to linkedSetOf(response.request.addonName)
                streamTypes[key] = streamType
            } else {
                entry.second += response.request.addonName
            }
        }
    }

    return grouped.map { (key, item) ->
        LiveTvChannelItem(
            key = key,
            meta = item.first,
            streamType = streamTypes.getValue(key),
            sourceNames = item.second.toList(),
        )
    }
}

private val LIVE_TV_CATALOG_TYPES = setOf("channel", "tv")
