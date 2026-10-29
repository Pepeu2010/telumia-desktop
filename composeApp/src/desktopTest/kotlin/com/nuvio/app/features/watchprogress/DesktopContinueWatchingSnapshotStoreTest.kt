package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.storage.DesktopStorage
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopContinueWatchingSnapshotStoreTest {
    @Test
    fun aggregateQuotaRejectsNewSnapshotButKeepsExistingOfflineSnapshot() = withStore { store ->
        val cache = cache(store, quota = 10)

        assertTrue(cache.write("profile-1", "123456"))
        assertFalse(cache.write("profile-2", "abcdef"))

        assertEquals("123456", cache.read("profile-1"))
        assertNull(cache.read("profile-2"))
    }

    @Test
    fun replacingSnapshotExcludesItsOldBytesFromAggregate() = withStore { store ->
        val cache = cache(store, quota = 10)

        assertTrue(cache.write("profile-1", "123456"))
        assertTrue(cache.write("profile-1", "12345678"))

        assertEquals("12345678", cache.read("profile-1"))
    }

    @Test
    fun readRemainsAvailableWhenStoragePreflightDeniesWrites() = withStore { store ->
        store.putString("cw_profile-1", "offline-snapshot")
        val cache = cache(store, quota = 100, canWrite = { false })

        assertEquals("offline-snapshot", cache.read("profile-1"))
        assertFalse(cache.write("profile-2", "new-snapshot"))
    }

    @Test
    fun writePreflightReceivesWholePreferencesRewriteEstimate() = withStore { store ->
        store.putString("cw_profile-1", "existing-snapshot")
        var checkedBytes = 0L
        val cache = cache(store, quota = 100, canWrite = { bytes -> checkedBytes = bytes; true })

        assertTrue(cache.write("profile-2", "new-snapshot"))
        assertTrue(checkedBytes >= store.persistedFileSizeBytes())
        assertTrue(checkedBytes >= "new-snapshot".length * 6L)
    }

    private fun cache(
        store: DesktopStorage.Store,
        quota: Long,
        canWrite: (Long) -> Boolean = { true },
    ) = DesktopContinueWatchingSnapshotStore(
        store = store,
        keyPrefix = "cw_",
        storageKey = { "cw_$it" },
        quotaBytes = { quota },
        canWriteEntry = canWrite,
        maxBytes = 100,
    )

    private fun withStore(block: (DesktopStorage.Store) -> Unit) {
        val directory = Files.createTempDirectory("telumia-cw-snapshot-")
        try {
            block(DesktopStorage.Store(directory.resolve("snapshots.properties")))
        } finally {
            Files.deleteIfExists(directory.resolve("snapshots.properties"))
            Files.deleteIfExists(directory)
        }
    }
}
