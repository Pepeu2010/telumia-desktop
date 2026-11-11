package com.nuvio.app.core.storage

import java.nio.file.Files
import java.nio.file.Path

/**
 * Removes credentials and caches that the retired Trakt integration left on disk.
 *
 * The tracking settings store keeps its original name and is intentionally not listed here.
 * Nuvio sign-in, Telumia Sync and local watch progress are never touched.
 */
internal object LegacyTraktDataCleaner {
    private val legacyStoreNames = listOf(
        "nuvio_trakt_auth",
        "nuvio_trakt_library",
        "nuvio_trakt_comments",
    )

    fun purge(root: Path = DesktopStorage.rootDir): Int = legacyStoreNames.count { name ->
        runCatching {
            // Properties stores are written through a temporary sibling file; remove both.
            val removed = Files.deleteIfExists(root.resolve("$name.properties"))
            Files.deleteIfExists(root.resolve("$name.properties.tmp"))
            removed
        }.getOrDefault(false)
    }
}
