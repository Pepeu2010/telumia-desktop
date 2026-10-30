package com.nuvio.app.features.tmdb

/** Initial entry-count budgets for the Desktop TMDB response caches. */
internal data class TmdbMetadataCacheLimits(
    val enrichment: Int,
    val episodes: Int,
    val recommendations: Int,
    val collections: Int,
    val trailers: Int,
    val people: Int,
    val entityBrowse: Int,
    val entityHeaders: Int,
    val entityRails: Int,
) {
    init {
        require(listOf(
            enrichment, episodes, recommendations, collections, trailers,
            people, entityBrowse, entityHeaders, entityRails,
        ).all { it > 0 }) { "TMDB metadata cache limits must be positive" }
    }

    companion object {
        /** Conservative default for unknown/low-capacity hosts; limits count keys, not bytes. */
        val DEFAULT = TmdbMetadataCacheLimits(
            enrichment = 24,
            episodes = 16,
            recommendations = 12,
            collections = 12,
            trailers = 12,
            people = 24,
            entityBrowse = 12,
            entityHeaders = 24,
            entityRails = 12,
        )
    }
}
