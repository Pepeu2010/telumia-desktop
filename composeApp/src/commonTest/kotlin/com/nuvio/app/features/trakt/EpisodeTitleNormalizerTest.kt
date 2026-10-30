package com.nuvio.app.features.trakt

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EpisodeTitleNormalizerTest {
    @Test
    fun `normalization preserves existing matching semantics`() = runTest {
        val normalizer = EpisodeTitleNormalizer(capacity = 2)

        assertEquals("a grande virada", normalizer.normalize("A Grande—Virada!"))
        assertEquals("a grande virada", normalizer.normalize("A Grande—Virada!"))
        assertEquals(1, normalizer.size())
    }

    @Test
    fun `normalized title cache remains within its entry budget`() = runTest {
        val normalizer = EpisodeTitleNormalizer(capacity = 2)
        normalizer.normalize("Primeiro episódio")
        normalizer.normalize("Segundo episódio")
        normalizer.normalize("Terceiro episódio")

        assertEquals(2, normalizer.size())
        assertEquals("terceiro epis dio", normalizer.normalize("Terceiro episódio"))
        assertEquals(2, normalizer.size())
    }

    @Test
    fun `clear removes cached normalized titles`() = runTest {
        val normalizer = EpisodeTitleNormalizer(capacity = 2)
        normalizer.normalize("Episódio piloto")

        normalizer.clear()

        assertEquals(0, normalizer.size())
    }
}
