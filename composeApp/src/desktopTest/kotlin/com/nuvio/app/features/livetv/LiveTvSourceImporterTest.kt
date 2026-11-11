package com.nuvio.app.features.livetv

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveTvSourceImporterTest {
    @Test fun importsPlaylistAndGuideWithTheSameSourceIdentity() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(playlist))
            server.enqueue(MockResponse().setBody(guide))
            val result = LiveTvSourceImporter { 1_800_000_000_000L }.load(source(server))
            val channel = result.channels.single()
            val program = result.programs.single()
            assertEquals("news.br", channel.tvgId)
            assertEquals(channel.tvgId, program.channelId)
            assertEquals(channel.sourceId, program.sourceId)
            assertEquals("Jornal", program.title)
            assertFalse(result.guideFailed)
            assertTrue(result.guideConfigured)
            assertEquals(1_800_000_000_000L, result.guideUpdatedAtEpochMillis)
        }
    }

    @Test fun failedGuideKeepsPlayableChannels() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(playlist))
            server.enqueue(MockResponse().setResponseCode(503))
            val result = LiveTvSourceImporter().load(source(server))
            assertTrue(result.channels.single().isPlayable)
            assertTrue(result.guideFailed)
            assertTrue(result.guideConfigured)
            assertTrue(result.programs.isEmpty())
            assertNull(result.guideUpdatedAtEpochMillis)
        }
    }

    @Test fun playlistWithoutXmlTvDoesNotClaimGuideWasUpdated() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(playlist))
            val source = source(server).copy(epgUrl = null)
            val result = LiveTvSourceImporter { 1_800_000_000_000L }.load(source)

            assertFalse(result.guideConfigured)
            assertNull(result.guideUpdatedAtEpochMillis)
            assertTrue(result.programs.isEmpty())
        }
    }

    private fun source(server: MockWebServer) = LiveTvSource(
        "test", "TV", server.url("/list.m3u").toString(), server.url("/guide.xml").toString(),
    )

    private val playlist = """
        #EXTM3U
        #EXTINF:-1 tvg-id="news.br",Notícias
        https://media.example/channel.m3u8
    """.trimIndent()

    private val guide = """
        <tv><programme channel="news.br" start="20260101180000 -0300" stop="20260101190000 -0300">
          <title lang="pt-BR">Jornal</title>
        </programme></tv>
    """.trimIndent()
}
