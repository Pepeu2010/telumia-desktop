package com.nuvio.app.core.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.compose.LocalPlatformContext
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.Path.Companion.toOkioPath

@OptIn(ExperimentalTestApi::class)
class CacheWritePreflightOfflineImageTest {
    @Test fun aPreviouslyCachedImageStillLoadsFromDiskWhenStoragePreflightBlocksWrites() = runDesktopComposeUiTest(width = 320, height = 240) {
        var platformContext: PlatformContext? = null
        setContent { platformContext = LocalPlatformContext.current }
        waitUntil(timeoutMillis = 5_000) { platformContext != null }
        val context = requireNotNull(platformContext)
        val testDirectory = Files.createTempDirectory("telumia-image-preflight")
        val server = MockWebServer()
        val storageAllowsWrites = AtomicBoolean(true)
        val clockMillis = AtomicLong(10L)
        val imageBytes = ByteArrayOutputStream().use { output ->
            ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", output)
            output.toByteArray()
        }
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(imageBytes))
                .addHeader("Content-Type", "image/png")
                .addHeader("Cache-Control", "public, max-age=3600"),
        )
        server.start()

        val loader = ImageLoader.Builder(context)
            .components {
                add(CacheWritePreflightInterceptor(clockMillis = { clockMillis.get() }) { storageAllowsWrites.get() })
                add(KtorNetworkFetcherFactory(cacheStrategy = { CacheControlCacheStrategy() }))
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(testDirectory.resolve("coil").toFile().toOkioPath())
                    .maxSizeBytes(4L * 1024L * 1024L)
                    .build()
            }
            .build()

        try {
            val request = ImageRequest.Builder(context)
                .data(server.url("/artwork.png").toString())
                .size(2, 2)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            val first = loader.execute(request)
            assertTrue(first is SuccessResult, "Fixture response should decode")
            assertEquals("NETWORK", (first as SuccessResult).dataSource.name)

            storageAllowsWrites.set(false)
            clockMillis.addAndGet(1_000L)
            server.shutdown()

            val offline = loader.execute(request)
            assertTrue(offline is SuccessResult, "Read-only policy should retain the offline disk hit")
            assertEquals("DISK", (offline as SuccessResult).dataSource.name)
            assertEquals(1, server.requestCount, "Only the initial online request should reach the fixture server")
        } finally {
            loader.shutdown()
            runCatching { server.shutdown() }
            testDirectory.toFile().deleteRecursively()
        }
    }
}
