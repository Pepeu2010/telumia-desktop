package com.nuvio.app.features.search

import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.HomeCatalogSection

enum class SearchEmptyStateReason {
    NoActiveAddons,
    NoSearchCatalogs,
    NoResults,
    RequestFailed,
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val sections: List<HomeCatalogSection> = emptyList(),
    val emptyStateReason: SearchEmptyStateReason? = null,
    val errorMessage: String? = null,
    val companyResults: List<SearchCompanyResult> = emptyList(),
    val isCompanySearchLoading: Boolean = false,
    val companySearchError: String? = null,
)

data class SearchCompanyResult(
    val tmdbId: Int,
    val name: String,
    val originCountry: String? = null,
)

internal fun deduplicateCompanySearchResults(results: Iterable<SearchCompanyResult>): List<SearchCompanyResult> =
    results.mapNotNull { result ->
        val name = result.name.trim().takeIf(String::isNotEmpty) ?: return@mapNotNull null
        if (result.tmdbId <= 0) return@mapNotNull null
        result.copy(name = name, originCountry = result.originCountry?.trim()?.takeIf(String::isNotEmpty))
    }.distinctBy(SearchCompanyResult::tmdbId)

internal fun SearchUiState.withCompanySearchLoading(clearResults: Boolean): SearchUiState = copy(
    companyResults = if (clearResults) emptyList() else companyResults,
    isCompanySearchLoading = true,
    companySearchError = null,
)

internal fun SearchUiState.withCompanySearchResults(results: Iterable<SearchCompanyResult>): SearchUiState = copy(
    companyResults = deduplicateCompanySearchResults(results),
    isCompanySearchLoading = false,
    companySearchError = null,
)

internal fun SearchUiState.withCompanySearchError(message: String): SearchUiState = copy(
    isCompanySearchLoading = false,
    companySearchError = message,
)

enum class DiscoverEmptyStateReason {
    NoActiveAddons,
    NoDiscoverCatalogs,
    NoResults,
    RequestFailed,
}

data class DiscoverCatalogOption(
    val key: String,
    val addonName: String,
    val manifestUrl: String,
    val type: String,
    val catalogId: String,
    val catalogName: String,
    val genreOptions: List<String> = emptyList(),
    val genreRequired: Boolean = false,
    val supportsPagination: Boolean = false,
)

data class DiscoverUiState(
    val typeOptions: List<String> = emptyList(),
    val selectedType: String? = null,
    val catalogOptions: List<DiscoverCatalogOption> = emptyList(),
    val selectedCatalogKey: String? = null,
    val selectedGenre: String? = null,
    val items: List<MetaPreview> = emptyList(),
    val isLoading: Boolean = false,
    val nextSkip: Int? = null,
    val consecutiveDuplicatePages: Int = 0,
    val emptyStateReason: DiscoverEmptyStateReason? = null,
    val errorMessage: String? = null,
) {
    val selectedCatalog: DiscoverCatalogOption?
        get() = catalogOptions.firstOrNull { it.key == selectedCatalogKey }

    val genreOptions: List<String>
        get() = selectedCatalog?.genreOptions.orEmpty()

    val canLoadMore: Boolean
        get() = nextSkip != null
}
