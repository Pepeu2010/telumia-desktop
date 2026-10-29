package com.nuvio.app.core.ui

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import com.nuvio.app.core.storage.DesktopMediaCache
import com.nuvio.app.core.storage.MediaCacheCategory
import okio.Path.Companion.toOkioPath

internal actual val platformProvidesImageLoader: Boolean = false

internal actual fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder {
    val openedQuota = DesktopMediaCache.coilImageQuota()
    return components {
        add(SkiaGifDecoder.Factory())
        add(CacheWritePreflightInterceptor {
            DesktopMediaCache.coilCacheWritesAllowed(MediaCacheCategory.IMAGES, openedQuota)
        })
    }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizeBytes(DesktopMediaCache.coilImageMemoryCacheBytes())
                .build()
        }
        .diskCachePolicy(CachePolicy.ENABLED)
        .diskCache {
        if (openedQuota <= 0L) null else DiskCache.Builder()
            .directory(DesktopMediaCache.directory(MediaCacheCategory.IMAGES).resolve("coil").toFile().toOkioPath())
            .maxSizeBytes(openedQuota).build()
    }
}

internal actual fun ImageLoader.Builder.configurePlatformPreviewImageLoader(): ImageLoader.Builder {
    val openedQuota = DesktopMediaCache.coilPreviewQuota()
    return components {
        add(CacheWritePreflightInterceptor {
            DesktopMediaCache.coilCacheWritesAllowed(MediaCacheCategory.PREVIEWS, openedQuota)
        })
    }
        .diskCachePolicy(CachePolicy.ENABLED)
        .diskCache {
            if (openedQuota <= 0L) null else DiskCache.Builder()
                .directory(DesktopMediaCache.directory(MediaCacheCategory.PREVIEWS).resolve("coil").toFile().toOkioPath())
                .maxSizeBytes(openedQuota)
                .build()
        }
}

internal actual fun ImageLoader.Builder.configurePlatformBadgeImageLoader(): ImageLoader.Builder {
    val openedQuota = DesktopMediaCache.coilBadgeQuota()
    return components {
        add(CacheWritePreflightInterceptor {
            DesktopMediaCache.coilCacheWritesAllowed(MediaCacheCategory.BADGES, openedQuota)
        })
    }
        .memoryCache {
        MemoryCache.Builder()
            .maxSizeBytes(DesktopMediaCache.coilBadgeMemoryCacheBytes())
            .build()
    }
        .diskCachePolicy(CachePolicy.ENABLED)
        .diskCache {
            if (openedQuota <= 0L) null else DiskCache.Builder()
                .directory(DesktopMediaCache.directory(MediaCacheCategory.BADGES).resolve("coil").toFile().toOkioPath())
                .maxSizeBytes(openedQuota)
                .build()
        }
}
