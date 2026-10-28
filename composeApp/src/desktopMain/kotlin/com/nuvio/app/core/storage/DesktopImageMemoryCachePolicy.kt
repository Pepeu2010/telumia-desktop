package com.nuvio.app.core.storage

/** Keeps decoded image memory bounded while scaling the cap with the host's physical RAM. */
internal object DesktopImageMemoryCachePolicy {
    private const val MIB = 1024L * 1024L
    private const val MEMORY_SHARE_DIVISOR = 100L
    private const val MIN_TOTAL_BYTES = 16L * MIB
    private const val MAX_TOTAL_BYTES = 96L * MIB
    private const val UNKNOWN_MEMORY_BYTES = 24L * MIB
    private const val BADGE_SHARE_DIVISOR = 8L

    data class Allocation(
        val totalBytes: Long,
        val imagesBytes: Long,
        val badgesBytes: Long,
    )

    fun allocate(physicalMemoryBytes: Long?): Allocation {
        val requestedBytes = physicalMemoryBytes
            ?.takeIf { it > 0L }
            ?.div(MEMORY_SHARE_DIVISOR)
            ?: UNKNOWN_MEMORY_BYTES
        val totalBytes = requestedBytes.coerceIn(MIN_TOTAL_BYTES, MAX_TOTAL_BYTES)
        val badgesBytes = totalBytes / BADGE_SHARE_DIVISOR
        return Allocation(
            totalBytes = totalBytes,
            imagesBytes = totalBytes - badgesBytes,
            badgesBytes = badgesBytes,
        )
    }
}
