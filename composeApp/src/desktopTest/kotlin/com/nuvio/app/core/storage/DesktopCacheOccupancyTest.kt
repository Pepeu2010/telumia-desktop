package com.nuvio.app.core.storage

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopCacheOccupancyTest {
    @Test
    fun `auto counts extracted torrent runtime but excludes durable torrent configuration`() = fixture { cache, data ->
        write(cache, "libmpv-probe.tmp", 2)
        write(cache, "media-cache-v1/previews/frame", 3)
        write(data, "nuvio_stream_link_cache.properties", 5)
        write(data, "torrserver/bin/windows/TorrServer.exe", 7)
        write(data, "torrserver/settings.db", 11)

        val result = measureDesktopCacheOccupancy(cache, data, maxMillis = 10_000)

        assertTrue(result.complete)
        assertEquals(17L, result.bytes)
        assertEquals(11L, Files.size(data.resolve("torrserver/settings.db")))
    }

    private fun fixture(test: (Path, Path) -> Unit) {
        val parent = Files.createTempDirectory("telumia-desktop-cache-occupancy-")
        val cache = Files.createDirectory(parent.resolve("cache"))
        val data = Files.createDirectory(parent.resolve("data"))
        try {
            test(cache, data)
        } finally {
            Files.walk(parent).use { files ->
                files.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    private fun write(root: Path, name: String, size: Int) {
        val file = root.resolve(name)
        Files.createDirectories(file.parent)
        Files.write(file, ByteArray(size))
    }
}
