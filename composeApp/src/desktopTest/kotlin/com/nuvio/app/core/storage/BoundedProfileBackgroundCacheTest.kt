package com.nuvio.app.core.storage

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoundedProfileBackgroundCacheTest {
    @Test fun legacyAndHashedFilesShareTheSameByteAndFileBudget() {
        val parent = Files.createTempDirectory("telumia-profile-background-budget")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        var now = 0L
        var writesEnabled = true
        val cache = BoundedProfileBackgroundCache(
            current,
            legacy,
            quotaBytes = { 8L },
            maxFiles = 2,
            canWrite = { writesEnabled },
            clock = { ++now },
        )

        assertTrue(cache.put("current", byteArrayOf(1, 1, 1, 1)) != null)
        val legacyFile = legacy.resolve("legacy.png").also {
            Files.write(it, byteArrayOf(2, 2, 2, 2))
            Files.setLastModifiedTime(it, FileTime.fromMillis(0))
        }
        writesEnabled = false
        assertEquals(legacyFile.toFile(), cache.getFile("legacy"))
        writesEnabled = true

        assertTrue(cache.put("new", byteArrayOf(3, 3, 3, 3)) != null)

        assertNull(cache.getFile("current"))
        assertTrue(legacyFile.toFile().isFile)
        assertEquals(BoundedProfileBackgroundCache.Usage(bytes = 8, files = 2), cache.usage())
    }

    @Test fun validLegacyFileMigratesOnlyAfterTheHashedCopyIsVerified() {
        val parent = Files.createTempDirectory("telumia-profile-background-migrate")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        val bytes = byteArrayOf(1, 2, 3, 4)
        val legacyFile = legacy.resolve("background-v1.png").also { Files.write(it, bytes) }
        val cache = BoundedProfileBackgroundCache(current, legacy, quotaBytes = { 16L })

        val migrated = cache.getFile("background-v1") ?: error("Expected the valid legacy image to migrate")

        assertTrue(migrated.toPath().startsWith(current))
        assertContentEquals(bytes, Files.readAllBytes(migrated.toPath()))
        assertFalse(Files.exists(legacyFile))
        assertEquals(BoundedProfileBackgroundCache.Usage(bytes = 4, files = 1), cache.usage())
    }

    @Test fun readableLegacyFileStaysAvailableWhenNewWritesAreDisabled() {
        val parent = Files.createTempDirectory("telumia-profile-background-readonly")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        val bytes = byteArrayOf(8, 7, 6)
        val legacyFile = legacy.resolve("offline.png").also { Files.write(it, bytes) }
        val cache = BoundedProfileBackgroundCache(
            current,
            legacy,
            quotaBytes = { 16L },
            canWrite = { false },
        )

        assertEquals(legacyFile.toFile(), cache.getFile("offline"))
        assertContentEquals(bytes, Files.readAllBytes(legacyFile))
        assertFalse(Files.exists(current))
        assertEquals(BoundedProfileBackgroundCache.Usage(bytes = 3, files = 1), cache.usage())
    }

    @Test fun oversizedLegacyFilesAreRemovedEvenWhenTheTotalQuotaWouldFit() {
        val parent = Files.createTempDirectory("telumia-profile-background-oversized")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        val legacyFile = legacy.resolve("large.png").also { Files.write(it, ByteArray(5)) }
        val cache = BoundedProfileBackgroundCache(
            current,
            legacy,
            quotaBytes = { 20L },
            maxEntryBytes = 4L,
        )

        assertNull(cache.getFile("large"))
        assertFalse(Files.exists(legacyFile))
        assertEquals(BoundedProfileBackgroundCache.Usage(bytes = 0, files = 0), cache.usage())
    }
}
