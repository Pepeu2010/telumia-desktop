package com.nuvio.app.core.storage

internal data class DesktopMetadataPreferenceCacheAllocation(
    val streamLinksBytes: Long,
    val bingeGroupsBytes: Long,
)

/** Suballocates the existing METADATA share without changing its device-adaptive parent budget. */
internal object DesktopMetadataPreferenceCachePolicy {
    const val MAX_STREAM_LINK_BYTES = 8L * MIB
    const val MAX_BINGE_GROUP_BYTES = 2L * MIB
    const val MAX_CONTINUE_WATCHING_BYTES = 8L * MIB

    fun allocate(categoryQuotaBytes: Long): DesktopMetadataPreferenceCacheAllocation {
        val quota = categoryQuotaBytes.coerceAtLeast(0L)
        val afterContinueWatching = (quota - quota.coerceAtMost(MAX_CONTINUE_WATCHING_BYTES)).coerceAtLeast(0L)
        val streamLinks = (afterContinueWatching - afterContinueWatching / 4L)
            .coerceAtMost(MAX_STREAM_LINK_BYTES)
        val afterStreamLinks = (afterContinueWatching - streamLinks).coerceAtLeast(0L)
        val bingeGroups = (afterStreamLinks - afterStreamLinks / 2L)
            .coerceAtMost(MAX_BINGE_GROUP_BYTES)
        return DesktopMetadataPreferenceCacheAllocation(streamLinks, bingeGroups)
    }
}
