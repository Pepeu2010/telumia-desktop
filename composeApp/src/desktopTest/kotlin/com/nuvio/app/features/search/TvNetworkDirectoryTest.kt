package com.nuvio.app.features.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TvNetworkDirectoryTest {
    @Test fun findsBrazilianNetworkByNameAndPreservesItsTmdbId() {
        assertTrue(TvNetworkDirectory.search("globo").any { it.id == 60 && it.name == "TV Globo" })
        assertEquals(emptyList(), TvNetworkDirectory.search("g"))
        assertTrue(TvNetworkDirectory.search("bbc").size <= 12)
    }
}
