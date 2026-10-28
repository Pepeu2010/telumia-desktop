package com.nuvio.app.core.storage

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectCacheFileOccupancyTest {
    private fun fixture(test: (Path) -> Unit) {
        val root = Files.createTempDirectory("telumia-direct-cache-occupancy-")
        try { test(root) } finally {
            Files.walk(root).use { files -> files.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        }
    }

    @Test fun countsOnlyRegularFilesDirectlyUnderCacheRoot() = fixture { root ->
        Files.write(root.resolve("flat-cache-entry"), ByteArray(13))
        Files.createDirectories(root.resolve("managed-category"))
        Files.write(root.resolve("managed-category/entry"), ByteArray(17))

        val result = DirectCacheFileOccupancy.measure(root, maxMillis = 10_000)

        assertTrue(result.complete)
        assertEquals(2, result.visitedEntries)
        assertEquals(13L, result.bytes)
        assertEquals(17L, Files.size(root.resolve("managed-category/entry")))
    }

    @Test fun missingCacheRootIsAnEmptyMeasurement() {
        val parent = Files.createTempDirectory("telumia-direct-cache-missing-")
        try {
            assertEquals(MediaCacheOccupancy(0, 0, true),
                DirectCacheFileOccupancy.measure(parent.resolve("not-created"), maxMillis = 10_000))
        } finally { Files.deleteIfExists(parent) }
    }

    @Test fun entryLimitProducesAnExplicitPartialMeasurement() = fixture { root ->
        repeat(10) { Files.write(root.resolve("entry-$it"), ByteArray(4)) }

        val result = DirectCacheFileOccupancy.measure(root, maxEntries = 3, maxMillis = 10_000)

        assertFalse(result.complete)
        assertEquals(3, result.visitedEntries)
        assertEquals(12L, result.bytes)
    }

    @Test fun doesNotFollowLinksOutsideCacheRoot() = fixture { root ->
        val outside = Files.createTempDirectory("telumia-direct-cache-outside-")
        val outsideFile = Files.write(outside.resolve("private-file"), ByteArray(101))
        val link = root.resolve("linked-entry")
        try {
            if (System.getProperty("os.name").lowercase().contains("windows")) {
                val process = ProcessBuilder("cmd.exe", "/c", "mklink", "/J", link.toString(), outside.toString())
                    .redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                assertEquals(output, 0, process.waitFor())
            } else {
                Files.createSymbolicLink(link, outsideFile)
            }

            val result = DirectCacheFileOccupancy.measure(root, maxMillis = 10_000)

            assertTrue(result.complete)
            assertEquals(0L, result.bytes)
            assertEquals(101L, Files.size(outsideFile))
        } finally {
            Files.deleteIfExists(link)
            Files.deleteIfExists(outsideFile)
            Files.deleteIfExists(outside)
        }
    }
}
