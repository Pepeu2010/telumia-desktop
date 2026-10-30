package com.nuvio.app.features.tmdb

import com.nuvio.app.features.membership.MemberAssetStorage

/** Entry-count limits for the reciprocal IMDB↔TMDB mapping caches. */
internal data class TmdbIdMappingCacheLimits(val entriesPerDirection: Int) {
    init {
        require(entriesPerDirection > 0) { "TMDB ID mapping cache capacity must be positive" }
    }

    companion object {
        private const val GIB = 1024L * 1024L * 1024L
        private const val LOW_RAM_MAX_GIB = 2L
        private const val BALANCED_MAX_GIB = 3L
        private const val LOW_RAM_ENTRIES = 128
        private const val BALANCED_ENTRIES = 256
        private const val HIGH_CAPACITY_ENTRIES = 512

        /** Unknown or low-capacity devices use the conservative initial limit. */
        val DEFAULT = forPhysicalMemoryBytes(MemberAssetStorage.profileBackgroundDeviceMemoryBytes())

        fun forPhysicalMemoryBytes(totalMemoryBytes: Long?): TmdbIdMappingCacheLimits {
            val entries = when {
                totalMemoryBytes == null || totalMemoryBytes <= 0L || totalMemoryBytes <= LOW_RAM_MAX_GIB * GIB ->
                    LOW_RAM_ENTRIES
                totalMemoryBytes <= BALANCED_MAX_GIB * GIB -> BALANCED_ENTRIES
                else -> HIGH_CAPACITY_ENTRIES
            }
            return TmdbIdMappingCacheLimits(entriesPerDirection = entries)
        }
    }
}
