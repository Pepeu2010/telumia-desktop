package com.nuvio.app.features.search

internal fun shouldShowSearchRecents(
    query: String,
    hasOpenedSearch: Boolean,
    recentSearchCount: Int,
): Boolean = query.isBlank() && hasOpenedSearch && recentSearchCount > 0
