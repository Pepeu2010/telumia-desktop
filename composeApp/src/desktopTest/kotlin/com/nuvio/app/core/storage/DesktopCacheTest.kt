package com.nuvio.app.core.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopCacheTest {
    @Test
    fun `install keeps the current assets and prunes only stale version directories`() {
        val cacheRoot = Files.createTempDirectory("desktop-versioned-assets-")
        try {
            val namespace = cacheRoot.resolve("player-ui")
            val staleVersion = namespace.resolve("a".repeat(24))
            Files.createDirectories(staleVersion)
            Files.writeString(staleVersion.resolve("controls.js"), "old controls")

            val unrelatedDirectory = namespace.resolve("operator-data")
            Files.createDirectories(unrelatedDirectory)
            Files.writeString(unrelatedDirectory.resolve("keep.txt"), "keep")

            val otherNamespaceVersion = cacheRoot.resolve("quickjs").resolve("b".repeat(24))
            Files.createDirectories(otherNamespaceVersion)
            Files.writeString(otherNamespaceVersion.resolve("runtime.dll"), "other namespace")

            val installed = DesktopCache.installVersionedFiles(
                cacheRoot = cacheRoot,
                namespace = "player-ui",
                files = mapOf("controls.js" to "current controls".toByteArray()),
            )

            assertEquals("current controls", Files.readString(installed.resolve("controls.js")))
            assertFalse(Files.exists(staleVersion))
            assertEquals("keep", Files.readString(unrelatedDirectory.resolve("keep.txt")))
            assertEquals("other namespace", Files.readString(otherNamespaceVersion.resolve("runtime.dll")))
        } finally {
            Files.walk(cacheRoot).use { paths ->
                paths.sorted(java.util.Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
