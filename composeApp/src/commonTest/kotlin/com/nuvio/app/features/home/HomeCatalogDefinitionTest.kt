package com.nuvio.app.features.home

import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.ManagedAddon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HomeCatalogDefinitionTest {
    private val definition = HomeCatalogDefinition(
        key = "addon:movie:popular",
        defaultTitle = "Popular - Movie",
        catalogName = "Popular",
        addonName = "Addon",
        manifestUrl = "https://example.com/manifest.json",
        type = "movie",
        catalogId = "popular",
        supportsPagination = true,
        descriptorSignature = "signature",
    )

    @Test
    fun `shows the type suffix by default`() {
        assertEquals("Popular - Movie", definition.titleFor(showCatalogType = true))
    }

    @Test
    fun `omits the type suffix when disabled`() {
        assertEquals("Popular", definition.titleFor(showCatalogType = false))
    }

    @Test
    fun `addon refresh signature tracks unresolved terminal and loaded manifest states`() {
        val pendingAddon = ManagedAddon(
            manifestUrl = "https://example.test/manifest.json",
            isRefreshing = true,
        )
        val failedAddon = pendingAddon.copy(
            isRefreshing = false,
            errorMessage = "Timed out",
        )
        val loadedAddon = pendingAddon.copy(
            manifest = AddonManifest(
                id = "addon",
                name = "Addon",
                description = "",
                version = "1.0.0",
                resources = listOf(AddonResource(name = "meta", types = listOf("movie"))),
                types = listOf("movie"),
                catalogs = emptyList(),
                transportUrl = pendingAddon.manifestUrl,
            ),
            isRefreshing = false,
        )

        val pendingSignature = buildAddonCatalogRefreshSignature(listOf(pendingAddon))
        val failedSignature = buildAddonCatalogRefreshSignature(listOf(failedAddon))
        val loadedSignature = buildAddonCatalogRefreshSignature(listOf(loadedAddon))

        assertTrue(pendingSignature.isNotEmpty())
        assertNotEquals(pendingSignature, failedSignature)
        assertNotEquals(pendingSignature, loadedSignature)
        assertNotEquals(failedSignature, loadedSignature)
    }

    @Test
    fun `home includes movie and series catalogs but excludes live tv catalogs`() {
        val addonUrl = "https://example.test/manifest.json"
        val addon = ManagedAddon(
            manifestUrl = addonUrl,
            manifest = AddonManifest(
                id = "mixed-catalogs",
                name = "Mixed catalogs",
                description = "",
                version = "1.0.0",
                resources = listOf(AddonResource(name = "meta", types = listOf("movie", "series", "channel", "tv"))),
                types = listOf("movie", "series", "channel", "tv"),
                catalogs = listOf(
                    AddonCatalog(type = "movie", id = "popular-movies", name = "Popular movies"),
                    AddonCatalog(type = "series", id = "popular-series", name = "Popular series"),
                    AddonCatalog(type = "channel", id = "live-channels", name = "Live channels"),
                    AddonCatalog(type = "TV", id = "live-tv", name = "Live TV"),
                ),
                transportUrl = addonUrl,
            ),
        )

        assertEquals(
            listOf("movie", "series"),
            buildHomeCatalogDefinitions(listOf(addon)).map(HomeCatalogDefinition::type),
        )
    }
}
