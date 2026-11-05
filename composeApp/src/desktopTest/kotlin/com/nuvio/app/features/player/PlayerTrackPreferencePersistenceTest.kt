package com.nuvio.app.features.player

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlayerTrackPreferencePersistenceTest {
    @Test fun forcedSelectionSurvivesStorageRoundTripAndCanBeCleared() {
        val id = "telumia-test-${UUID.randomUUID()}"
        try {
            assertNull(PlayerTrackPreferenceStorage.load(id))
            for (forced in listOf(true, false)) {
                val preference = PersistedPlayerTrackPreference(
                    subtitleLanguage = "pt-BR", subtitleIsForced = forced,
                    audioLanguage = "pt-BR",
                )
                PlayerTrackPreferenceStorage.save(id, preference)
                assertEquals(preference, PlayerTrackPreferenceStorage.load(id))
            }
            PlayerTrackPreferenceStorage.save(id, PersistedPlayerTrackPreference(subtitleIsForced = false))
            assertEquals(false, assertNotNull(PlayerTrackPreferenceStorage.load(id)).subtitleIsForced)
            PlayerTrackPreferenceStorage.save(id, PersistedPlayerTrackPreference())
            assertNull(PlayerTrackPreferenceStorage.load(id))
        } finally {
            PlayerTrackPreferenceStorage.save(id, PersistedPlayerTrackPreference())
        }
    }
}
