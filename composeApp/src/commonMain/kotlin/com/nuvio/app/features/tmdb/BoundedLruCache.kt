package com.nuvio.app.features.tmdb

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Small suspending LRU for response objects; only the configured number of values stay resident. */
internal class BoundedLruCache<K : Any, V : Any>(private val capacity: Int) {
    private val mutex = Mutex()
    private val entries = linkedMapOf<K, V>()

    init { require(capacity > 0) { "Cache capacity must be positive" } }

    suspend fun get(key: K): V? = mutex.withLock {
        entries.remove(key)?.also { value -> entries[key] = value }
    }

    suspend fun put(key: K, value: V) = mutex.withLock {
        entries.remove(key)
        entries[key] = value
        while (entries.size > capacity) {
            val oldestKey = entries.keys.first()
            entries.remove(oldestKey)
        }
    }

    suspend fun clear() = mutex.withLock { entries.clear() }

    suspend fun size(): Int = mutex.withLock { entries.size }
}
