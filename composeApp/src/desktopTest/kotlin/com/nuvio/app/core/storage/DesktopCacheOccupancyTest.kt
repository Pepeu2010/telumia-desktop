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

    @Test
    fun `auto counts staged updater package but excludes user downloads`() = fixture { cache, data ->
        write(data, "updates/Telumia-setup.msi", 13)
        write(data, "downloads/movie.mkv", 17)

        val result = measureDesktopCacheOccupancy(cache, data, maxMillis = 10_000)

        assertTrue(result.complete)
        assertEquals(13L, result.bytes)
        assertEquals(17L, Files.size(data.resolve("downloads/movie.mkv")))
    }

    @Test
    fun `auto counts extracted bundled profile art but excludes selected user avatars`() = fixture { cache, data ->
        write(data, "profile-studio-v1/bundled/ABCD.svg", 19)
        write(data, "profile-studio-v1/avatars/profile-1.png", 23)

        val result = measureDesktopCacheOccupancy(cache, data, maxMillis = 10_000)

        assertTrue(result.complete)
        assertEquals(19L, result.bytes)
        assertEquals(23L, Files.size(data.resolve("profile-studio-v1/avatars/profile-1.png")))
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
