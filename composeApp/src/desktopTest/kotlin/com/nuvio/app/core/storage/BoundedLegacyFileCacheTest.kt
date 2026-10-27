package com.nuvio.app.core.storage

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoundedLegacyFileCacheTest {
    @Test fun legacyAndHashedFilesShareTheSameByteAndFileBudget() {
        val parent = Files.createTempDirectory("telumia-profile-background-budget")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        var now = 0L
        var writesEnabled = true
        val cache = BoundedLegacyFileCache(
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
        assertEquals(BoundedLegacyFileCache.Usage(bytes = 8, files = 2), cache.usage())
    }

    @Test fun validLegacyFileMigratesOnlyAfterTheHashedCopyIsVerified() {
        val parent = Files.createTempDirectory("telumia-profile-background-migrate")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        val bytes = byteArrayOf(1, 2, 3, 4)
        val legacyFile = legacy.resolve("background-v1.png").also { Files.write(it, bytes) }
        val cache = BoundedLegacyFileCache(current, legacy, quotaBytes = { 16L })

        val migrated = cache.getFile("background-v1") ?: error("Expected the valid legacy image to migrate")

        assertTrue(migrated.toPath().startsWith(current))
        assertContentEquals(bytes, Files.readAllBytes(migrated.toPath()))
        assertFalse(Files.exists(legacyFile))
        assertEquals(BoundedLegacyFileCache.Usage(bytes = 4, files = 1), cache.usage())
    }

    @Test fun readableLegacyFileStaysAvailableWhenNewWritesAreDisabled() {
        val parent = Files.createTempDirectory("telumia-profile-background-readonly")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        val bytes = byteArrayOf(8, 7, 6)
        val legacyFile = legacy.resolve("offline.png").also { Files.write(it, bytes) }
        val cache = BoundedLegacyFileCache(
            current,
            legacy,
            quotaBytes = { 16L },
            canWrite = { false },
        )

        assertEquals(legacyFile.toFile(), cache.getFile("offline"))
        assertContentEquals(bytes, Files.readAllBytes(legacyFile))
        assertFalse(Files.exists(current))
        assertEquals(BoundedLegacyFileCache.Usage(bytes = 3, files = 1), cache.usage())
    }

    @Test fun oversizedLegacyFilesAreRemovedEvenWhenTheTotalQuotaWouldFit() {
        val parent = Files.createTempDirectory("telumia-profile-background-oversized")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        val legacyFile = legacy.resolve("large.png").also { Files.write(it, ByteArray(5)) }
        val cache = BoundedLegacyFileCache(
            current,
            legacy,
            quotaBytes = { 20L },
            maxEntryBytes = 4L,
        )

        assertNull(cache.getFile("large"))
        assertFalse(Files.exists(legacyFile))
        assertEquals(BoundedLegacyFileCache.Usage(bytes = 0, files = 0), cache.usage())
    }

    @Test fun existingHashedHitRemainsReadableWhenTheDynamicQuotaFallsToZero() {
        val parent = Files.createTempDirectory("telumia-legacy-file-readonly-hit")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        var quota = 16L
        var writesEnabled = true
        val cache = BoundedLegacyFileCache(
            current,
            legacy,
            quotaBytes = { quota },
            canWrite = { writesEnabled },
        )
        val bytes = byteArrayOf(5, 4, 3)
        assertTrue(cache.put("offline", bytes) != null)

        quota = 0
        writesEnabled = false

        assertContentEquals(bytes, cache.get("offline"))
        assertEquals(BoundedLegacyFileCache.Usage(bytes = 3, files = 1), cache.usage())
        assertNull(cache.put("new", byteArrayOf(8)))
    }

    @Test fun legacyGifFileMigratesFromItsOldUrlHashToTheSharedHashedCache() {
        val parent = Files.createTempDirectory("telumia-legacy-gif-migrate")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        val url = "https://images.example/gif?id=43"
        val bytes = byteArrayOf(71, 73, 70, 56, 57, 97)
        val legacyFile = legacy.resolve("${sha256(url)}.gif").also { Files.write(it, bytes) }
        val cache = BoundedLegacyFileCache(
            current,
            legacy,
            quotaBytes = { 16L },
            maxFiles = 2,
            maxEntryBytes = 8L,
            legacyFileName = { key -> "${sha256(key)}.gif" },
            legacyName = Regex("[a-f0-9]{64}\\.gif"),
        )

        assertContentEquals(bytes, cache.get(url))
        assertFalse(Files.exists(legacyFile))
        assertEquals(BoundedLegacyFileCache.Usage(bytes = bytes.size.toLong(), files = 1), cache.usage())
    }

    @Test fun legacyGifKeyUsesTheRecognizedHashedFilenameAndStaysReadableOffline() {
        val parent = Files.createTempDirectory("telumia-legacy-gif")
        val current = parent.resolve("hashed")
        val legacy = parent.resolve("legacy")
        Files.createDirectories(legacy)
        val url = "https://images.example/gif?id=42"
        val bytes = byteArrayOf(71, 73, 70, 56, 57, 97)
        val legacyFile = legacy.resolve("${sha256(url)}.gif").also { Files.write(it, bytes) }
        val cache = BoundedLegacyFileCache(
            current,
            legacy,
            quotaBytes = { 0L },
            maxFiles = 2,
            maxEntryBytes = 8L,
            canWrite = { false },
            legacyFileName = { key -> "${sha256(key)}.gif" },
            legacyName = Regex("[a-f0-9]{64}\\.gif"),
        )

        assertContentEquals(bytes, cache.get(url))
        assertTrue(Files.isRegularFile(legacyFile))
        assertEquals(BoundedLegacyFileCache.Usage(bytes = bytes.size.toLong(), files = 1), cache.usage())
    }

    private fun sha256(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
