package com.nuvio.app.features.livetv

import com.nuvio.app.features.catalog.CatalogPage
import com.nuvio.app.features.catalog.mergeCatalogItems
import com.nuvio.app.features.catalog.nextCatalogPaginationState
import com.nuvio.app.features.catalog.supportsPagination
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Publishes each merged page; callers retain usable channels if a later request fails. */
internal suspend fun loadLiveTvCatalogPages(
    request: LiveTvCatalogRequest,
    fetchPage: suspend (skip: Int?) -> CatalogPage,
    onPage: (LiveTvCatalogResponse) -> Unit,
): LiveTvCatalogResponse {
    var items = emptyList<MetaPreview>()
    var skip: Int? = null
    var duplicates = 0
    do {
        currentCoroutineContext().ensureActive()
        val page = fetchPage(skip)
        val merged = mergeCatalogItems(items, page.items)
        val state = nextCatalogPaginationState(
            supportsPagination = request.catalog.supportsPagination(),
            requestedSkip = skip ?: 0,
            page = page,
            loadedNewItems = merged.size > items.size,
            consecutiveDuplicatePages = duplicates,
        )
        items = merged
        onPage(LiveTvCatalogResponse(request, items))
        val next = state.nextSkip
        // A broken cursor must never trigger an endless sequence of requests.
        skip = next?.takeIf { it > (skip ?: 0) }
        duplicates = state.consecutiveDuplicatePages
    } while (skip != null)
    return LiveTvCatalogResponse(request, items)
}
