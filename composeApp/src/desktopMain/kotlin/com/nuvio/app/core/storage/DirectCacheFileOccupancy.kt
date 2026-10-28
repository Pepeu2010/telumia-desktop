package com.nuvio.app.core.storage

import java.io.IOException
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

/** Read-only measurement of regular files directly under the app cache root. */
internal object DirectCacheFileOccupancy {
    fun measure(
        cacheDirectory: Path,
        maxEntries: Int = 16_384,
        maxMillis: Long = 25,
    ): MediaCacheOccupancy {
        require(maxEntries > 0 && maxMillis > 0)
        val base = try { cacheDirectory.toRealPath() }
            catch (_: NoSuchFileException) { return MediaCacheOccupancy(0, 0, true) }
            catch (_: IOException) { return MediaCacheOccupancy(0, 0, false) }
            catch (_: SecurityException) { return MediaCacheOccupancy(0, 0, false) }

        val started = System.nanoTime()
        var bytes = 0L
        var visited = 0
        var complete = true
        val fileKeys = HashSet<Any>()
        try {
            Files.newDirectoryStream(base).use { entries ->
                val iterator = entries.iterator()
                while (iterator.hasNext()) {
                    if (visited >= maxEntries || System.nanoTime() - started >= maxMillis * 1_000_000L) {
                        complete = false
                        break
                    }
                    val path = iterator.next()
                    visited++
                    try {
                        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
                        if (attributes.isSymbolicLink || attributes.isOther || !attributes.isRegularFile) continue
                        // Reject a file replaced by a link while the directory stream was open.
                        if (!path.toRealPath().startsWith(base)) continue
                        val key = attributes.fileKey()
                        if (key == null || fileKeys.add(key)) {
                            val size = attributes.size().coerceAtLeast(0)
                            bytes = if (bytes > Long.MAX_VALUE - size) Long.MAX_VALUE else bytes + size
                        }
                    } catch (_: IOException) { complete = false }
                      catch (_: SecurityException) { complete = false }
                }
            }
        } catch (_: NoSuchFileException) {
            // A new install may lose an empty cache root between resolving it and opening it.
        } catch (_: IOException) { complete = false }
          catch (_: SecurityException) { complete = false }
          catch (_: DirectoryIteratorException) { complete = false }

        return MediaCacheOccupancy(bytes, visited, complete)
    }
}
