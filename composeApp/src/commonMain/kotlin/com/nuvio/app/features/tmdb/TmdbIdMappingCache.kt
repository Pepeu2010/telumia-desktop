package com.nuvio.app.features.tmdb

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps reciprocal ID lookups bounded independently in each direction. */
internal class TmdbIdMappingCache(capacityPerDirection: Int) {
    private val mutex = Mutex()
    private val imdbToTmdb = BoundedLruCache<String, String>(capacityPerDirection)
    private val tmdbToImdb = BoundedLruCache<String, String>(capacityPerDirection)

    suspend fun tmdbIdForImdb(imdbId: String, mediaType: String): String? =
        mutex.withLock { imdbToTmdb.get(key(imdbId, mediaType)) }

    suspend fun imdbIdForTmdb(tmdbId: String, mediaType: String): String? =
        mutex.withLock { tmdbToImdb.get(key(tmdbId, mediaType)) }

    suspend fun remember(imdbId: String, tmdbId: String, mediaType: String) {
        mutex.withLock {
            imdbToTmdb.put(key(imdbId, mediaType), tmdbId)
            tmdbToImdb.put(key(tmdbId, mediaType), imdbId)
        }
    }

    private fun key(id: String, mediaType: String): String =
        "$id:${normalizeTmdbMediaType(mediaType)}"
}
