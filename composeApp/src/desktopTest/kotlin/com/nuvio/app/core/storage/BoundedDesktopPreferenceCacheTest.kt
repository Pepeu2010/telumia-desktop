package com.nuvio.app.core.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class BoundedDesktopPreferenceCacheTest {
    @Test
    fun `save evicts oldest entries and leaves unrelated preferences intact`() = withStore { store ->
        store.putString("stream_link_old", "1")
        store.putString("unrelated_setting", "keep")
        var now = 2L
        val cache = BoundedDesktopPreferenceCache(
            store = store,
            namespacePrefix = "stream_link_",
            entryPrefix = "stream_link_",
            maxEntries = 2,
            quotaBytes = { Long.MAX_VALUE },
            canWriteEntry = { true },
            savedAtMs = { _, value -> value.toLongOrNull() ?: 0L },
            nowMs = { now++ },
        )

        cache.save("stream_link_new", "2")
        cache.save("stream_link_newest", "3")

        assertEquals(null, cache.load("stream_link_old"))
        assertEquals("2", cache.load("stream_link_new"))
        assertEquals("3", cache.load("stream_link_newest"))
        assertEquals("keep", store.getString("unrelated_setting"))
    }

    @Test
    fun `oversized candidate preserves existing cache hits`() = withStore { store ->
        store.putString("stream_link_old", "old-value")
        val cache = BoundedDesktopPreferenceCache(
            store = store,
            namespacePrefix = "stream_link_",
            entryPrefix = "stream_link_",
            maxEntries = 8,
            quotaBytes = { 1L },
            canWriteEntry = { true },
            savedAtMs = { _, _ -> 1L },
            nowMs = { 2L },
        )

        cache.save("stream_link_new", "new-value")

        assertEquals("old-value", cache.load("stream_link_old"))
        assertEquals(null, cache.load("stream_link_new"))
    }

    @Test
    fun `remove deletes the value and its profile timestamp in one namespace update`() = withStore { store ->
        store.putString("binge_group_hash_1", "group")
        store.putString("binge_saved_at_hash_1", "42")
        val cache = BoundedDesktopPreferenceCache(
            store = store,
            namespacePrefix = "binge_",
            entryPrefix = "binge_group_",
            timestampPrefix = "binge_saved_at_",
            timestampKeyFor = { key -> key.replaceFirst("binge_group_", "binge_saved_at_") },
            maxEntries = 8,
            quotaBytes = { Long.MAX_VALUE },
            canWriteEntry = { true },
            savedAtMs = { _, _ -> 0L },
            nowMs = { 43L },
        )

        cache.remove("binge_group_hash_1")

        assertEquals(null, store.getString("binge_group_hash_1"))
        assertEquals(null, store.getString("binge_saved_at_hash_1"))
    }

    private fun withStore(action: (DesktopStorage.Store) -> Unit) {
        val directory = Files.createTempDirectory("bounded-desktop-preferences-")
        try {
            action(DesktopStorage.Store(directory.resolve("cache.properties")))
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
