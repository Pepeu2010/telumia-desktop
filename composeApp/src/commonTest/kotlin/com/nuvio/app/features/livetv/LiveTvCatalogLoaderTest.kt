package com.nuvio.app.features.livetv

import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonExtraProperty
import com.nuvio.app.features.catalog.CatalogPage
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LiveTvCatalogLoaderTest {
    private fun request(paginated: Boolean = true) = LiveTvCatalogRequest(
        "https://tv.example/manifest.json", "TV",
        AddonCatalog("tv", "channels", "Channels",
            if (paginated) listOf(AddonExtraProperty("skip")) else emptyList()),
    )
    private fun page(vararg ids: Int, next: Int?) = CatalogPage(
        ids.map { MetaPreview("channel:$it", "tv", "Canal $it") }, ids.size, next,
    )

    @Test fun pagesArePublishedProgressivelyAndDuplicatesAreMerged() = runTest {
        val skips = mutableListOf<Int?>()
        val sizes = mutableListOf<Int>()
        val result = loadLiveTvCatalogPages(request(), { skip ->
            skips += skip
            when (skip) {
                null -> page(1, 2, next = 2)
                2 -> page(2, 3, next = 4)
                else -> page(next = null)
            }
        }, { sizes += it.items.size })
        assertEquals(listOf(null, 2, 4), skips)
        assertEquals(listOf(2, 3, 3), sizes)
        assertEquals(3, result.items.size)
    }

    @Test fun nonPaginatedAddonIsOnlyRequestedOnce() = runTest {
        var calls = 0
        loadLiveTvCatalogPages(request(false), { calls++; page(1, next = 1) }, {})
        assertEquals(1, calls)
    }

    @Test fun addonIgnoringSkipStopsAfterRepeatedDuplicatePages() = runTest {
        var calls = 0
        val result = loadLiveTvCatalogPages(request(), { skip ->
            calls++; page(1, next = (skip ?: 0) + 1)
        }, {})
        assertEquals(4, calls)
        assertEquals(1, result.items.size)
    }

    @Test fun laterFailurePreservesThePreviouslyPublishedPage() = runTest {
        var published: LiveTvCatalogResponse? = null
        assertFailsWith<IllegalStateException> {
            loadLiveTvCatalogPages(request(), { skip ->
                if (skip != null) error("Offline")
                page(1, next = 1)
            }, { published = it })
        }
        assertEquals(1, published?.items?.size)
    }

    @Test fun cancellationIsPropagated() = runTest {
        assertFailsWith<CancellationException> {
            loadLiveTvCatalogPages(request(), { throw CancellationException("Left TV") }, {})
        }
    }
}
