package com.nuvio.app.features.trakt

import com.nuvio.app.features.membership.MemberAssetStorage

/** Initial entry-count budgets for Desktop's Trakt episode mapping caches. */
internal data class TraktEpisodeMappingCacheLimits(
    val remappedEpisodes: Int,
    val addonEpisodeLists: Int,
    val traktEpisodeLists: Int,
    val reverseRemaps: Int,
    val normalizedEpisodeTitles: Int,
) {
    init {
        require(
            remappedEpisodes > 0 && addonEpisodeLists > 0 && traktEpisodeLists > 0 &&
                reverseRemaps > 0 && normalizedEpisodeTitles > 0,
        ) { "Trakt episode mapping cache limits must be positive" }
    }

    companion object {
        private const val GIB = 1024L * 1024L * 1024L

        /** Desktop derives the tier from physical host RAM; missing data uses LOW_RAM. */
        val DEFAULT = forPhysicalMemoryBytes(MemberAssetStorage.profileBackgroundDeviceMemoryBytes())

        fun forPhysicalMemoryBytes(totalMemoryBytes: Long?): TraktEpisodeMappingCacheLimits = when {
            totalMemoryBytes == null || totalMemoryBytes <= 0L || totalMemoryBytes <= 2L * GIB ->
                TraktEpisodeMappingCacheLimits(96, 4, 4, 96, 512)
            totalMemoryBytes <= 3L * GIB ->
                TraktEpisodeMappingCacheLimits(192, 8, 8, 192, 1_024)
            else -> TraktEpisodeMappingCacheLimits(384, 16, 16, 384, 2_048)
        }
    }
}
