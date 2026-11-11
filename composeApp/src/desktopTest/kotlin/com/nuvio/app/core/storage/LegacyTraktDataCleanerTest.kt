package com.nuvio.app.core.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegacyTraktDataCleanerTest {
    @Test
    fun `purge removes retired stores and keeps tracking settings`() {
        val root = Files.createTempDirectory("legacy-trakt-cleaner")
        try {
            val retired = listOf(
                "nuvio_trakt_auth.properties",
                "nuvio_trakt_library.properties",
                "nuvio_trakt_comments.properties",
            )
            val kept = listOf("nuvio_trakt_settings.properties", "nuvio_auth.properties", "nuvio_watch_progress.properties")
            (retired + kept).forEach { Files.writeString(root.resolve(it), "x") }

            assertEquals(retired.size, LegacyTraktDataCleaner.purge(root))

            retired.forEach { assertFalse(Files.exists(root.resolve(it)), it) }
            kept.forEach { assertTrue(Files.exists(root.resolve(it)), it) }
            assertEquals(0, LegacyTraktDataCleaner.purge(root))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
