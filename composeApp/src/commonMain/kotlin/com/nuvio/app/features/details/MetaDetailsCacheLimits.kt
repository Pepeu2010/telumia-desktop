package com.nuvio.app.features.details

import com.nuvio.app.features.membership.MemberAssetStorage

/** Initial in-memory entry budgets for complete metadata retained by the details repository. */
internal data class MetaDetailsCacheLimits(val entries: Int) {
    init {
        require(entries > 0) { "Metadata details cache capacity must be positive" }
    }

    companion object {
        private const val GIB = 1024L * 1024L * 1024L

        /** Host physical RAM selects a conservative default when the platform cannot report it. */
        val DEFAULT = forPhysicalMemoryBytes(MemberAssetStorage.profileBackgroundDeviceMemoryBytes())

        fun forPhysicalMemoryBytes(totalMemoryBytes: Long?): MetaDetailsCacheLimits = when {
            totalMemoryBytes == null || totalMemoryBytes <= 0L || totalMemoryBytes <= 4L * GIB ->
                MetaDetailsCacheLimits(entries = 8)
            totalMemoryBytes <= 8L * GIB -> MetaDetailsCacheLimits(entries = 16)
            else -> MetaDetailsCacheLimits(entries = 24)
        }
    }
}
