package com.nuvio.app.features.player

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assume.assumeTrue
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerSourceSelectionStorageTest {
    @Test fun `source strategy persists locally without exporting or accepting it from original account sync`() {
        assumeTrue(System.getenv("NUVIO_ENHANCED_ISOLATED_THEME_TEST") == "1")
        assertTrue(DesktopStorage.rootDir.startsWith(Paths.get(System.getenv("APPDATA")).resolve("Telumia")))
        PlayerSettingsStorage.saveStreamAutoPlayMode("FIRST_STREAM")
        PlayerSettingsStorage.saveStreamAutoPlayMode("BEST_QUALITY")
        assertEquals("BEST_QUALITY", PlayerSettingsStorage.loadStreamAutoPlayMode())
        val exported = PlayerSettingsStorage.exportToSyncPayload()
        assertEquals("FIRST_STREAM", exported.decodeSyncString("stream_auto_play_mode"))
        assertFalse("telumia_source_mode_v1" in exported)
        PlayerSettingsStorage.replaceFromSyncPayload(buildJsonObject {
            put("stream_auto_play_mode", encodeSyncString("REGEX_MATCH"))
            put("telumia_source_mode_v1", encodeSyncString("DATA_SAVER"))
        })
        assertEquals("BEST_QUALITY", PlayerSettingsStorage.loadStreamAutoPlayMode())
        assertEquals("REGEX_MATCH", PlayerSettingsStorage.exportToSyncPayload().decodeSyncString("stream_auto_play_mode"))
        PlayerSettingsStorage.saveStreamAutoPlayMode("MANUAL")
        assertEquals("MANUAL", PlayerSettingsStorage.loadStreamAutoPlayMode())
        assertEquals("MANUAL", PlayerSettingsStorage.exportToSyncPayload().decodeSyncString("stream_auto_play_mode"))
    }
}
