package com.nuvio.app.features.livetv

import com.nuvio.app.core.storage.DesktopStorage
import java.nio.file.Files
import kotlin.test.*

class LiveTvSourceStoreTest {
    @Test fun sourcesSurviveReopeningAndRemainIsolatedAcrossProfiles() {
        val file = Files.createTempDirectory("telumia-sources").resolve("sources.properties")
        var profile = 1
        fun open(): LiveTvSourceStore {
            val storage = DesktopStorage.Store(file)
            return LiveTvSourceStore({ profile }, { storage.getString("sources_$it") },
                { id, value -> storage.putString("sources_$id", value) })
        }
        try {
            val first = open().add(" Brasil ", "https://source.example/br.m3u?token=local", "https://source.example/epg.xml")
            assertEquals(listOf(first), open().list())
            profile = 2
            assertTrue(open().list().isEmpty())
            val second = open().add("Outro", "https://other.example/list.m3u")
            open().setEnabled(second.id, false)
            assertFalse(open().list().single().enabled)
            open().remove(second.id)
            assertTrue(open().list().isEmpty())
            profile = 1
            assertEquals(listOf(first), open().list())
        } finally {
            Files.deleteIfExists(file)
            Files.deleteIfExists(file.parent)
        }
    }

    @Test fun invalidUrlsAndSourceLimitDoNotChangeExistingConfiguration() {
        var payload: String? = null
        val store = LiveTvSourceStore({ 1 }, { payload }, { _, value -> payload = value })
        for (url in listOf("file:///private", "https://user:password@example.com/list", "https://example.com/list#fragment")) {
            assertFailsWith<IllegalArgumentException> { store.add("TV", url) }
        }
        assertNull(payload)
        repeat(20) { store.add("TV $it", "https://example.com/$it.m3u") }
        val before = payload
        assertFailsWith<IllegalArgumentException> { store.add("Excesso", "https://example.com/extra") }
        assertEquals(before, payload)
    }

    @Test fun corruptSavedDataIsNeverSilentlyReplacedByAnEdit() {
        var payload = "{invalid"
        val store = LiveTvSourceStore({ 1 }, { payload }, { _, value -> payload = value })
        assertFailsWith<IllegalStateException> { store.add("TV", "https://example.com/list") }
        assertEquals("{invalid", payload)
    }
}
