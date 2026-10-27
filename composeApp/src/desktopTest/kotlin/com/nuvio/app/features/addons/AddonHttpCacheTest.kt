package com.nuvio.app.features.addons

import java.nio.file.Files
import java.util.Comparator
import okhttp3.Cache
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AddonHttpCacheTest {
    private fun withCache(test: (Cache) -> Unit) {
        val directory = Files.createTempDirectory("telumia-desktop-addon-http-cache-")
        val cache = Cache(directory.toFile(), 1024L * 1024L)
        try {
            test(cache)
        } finally {
            cache.close()
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }

    @Test fun readonlyPolicyKeepsCachedAddonResponsesReadable() = withCache { cache ->
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("catalog").addHeader("Cache-Control", "public, max-age=3600"))
            var writesEnabled = true
            val client = createAddonHttpClient(cache) { writesEnabled }
            val request = okhttp3.Request.Builder().url(server.url("/catalog")).build()

            client.newCall(request).execute().use { assertEquals("catalog", it.body!!.string()) }
            writesEnabled = false
            client.newCall(request).execute().use { response ->
                assertEquals("catalog", response.body!!.string())
                assertNotNull(response.cacheResponse)
                assertNull(response.networkResponse)
            }
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun readonlyPolicyDoesNotPersistNewAddonResponses() = withCache { cache ->
        MockWebServer().use { server ->
            repeat(2) { server.enqueue(MockResponse().setBody("catalog").addHeader("Cache-Control", "public, max-age=3600")) }
            val client = createAddonHttpClient(cache) { false }
            val request = okhttp3.Request.Builder().url(server.url("/catalog")).build()

            repeat(2) {
                client.newCall(request).execute().use { response ->
                    assertEquals("catalog", response.body!!.string())
                    assertEquals("no-store", response.header("Cache-Control"))
                    assertNull(response.cacheResponse)
                }
            }
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun addonErrorResponsesAreNotCached() = withCache { cache ->
        MockWebServer().use { server ->
            repeat(2) { server.enqueue(MockResponse().setResponseCode(503).setBody("offline").addHeader("Cache-Control", "public, max-age=3600")) }
            val client = createAddonHttpClient(cache) { true }
            val request = okhttp3.Request.Builder().url(server.url("/catalog")).build()

            repeat(2) {
                client.newCall(request).execute().use { response ->
                    assertEquals(503, response.code)
                    assertEquals("no-store", response.header("Cache-Control"))
                    assertNull(response.cacheResponse)
                }
            }
            assertEquals(2, server.requestCount)
        }
    }
}
