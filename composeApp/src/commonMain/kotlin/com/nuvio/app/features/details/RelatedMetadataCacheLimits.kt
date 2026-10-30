package com.nuvio.app.features.details

import com.nuvio.app.features.membership.MemberAssetStorage

/** Initial response counts for related-title caches shared by tracking providers. */
internal data class RelatedMetadataCacheLimits(val entries: Int) {
    init {
        require(entries > 0) { "Related metadata cache capacity must be positive" }
    }

    companion object {
        private const val GIB = 1024L * 1024L * 1024L

        val DEFAULT = forPhysicalMemoryBytes(MemberAssetStorage.profileBackgroundDeviceMemoryBytes())

        fun forPhysicalMemoryBytes(totalMemoryBytes: Long?): RelatedMetadataCacheLimits = when {
            totalMemoryBytes == null || totalMemoryBytes <= 0L || totalMemoryBytes <= 4L * GIB ->
                RelatedMetadataCacheLimits(entries = 12)
            totalMemoryBytes <= 8L * GIB -> RelatedMetadataCacheLimits(entries = 24)
            else -> RelatedMetadataCacheLimits(entries = 48)
        }
    }
}
