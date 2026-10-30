package com.nuvio.app.core.cache

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** Thread-safe LRU that expires entries on access and bounds retained values. */
internal class TimedBoundedLruCache<K : Any, V : Any>(
    private val capacity: Int,
    private val ttlMs: Long,
) {
    private data class Entry<V>(val value: V, val updatedAtMs: Long)

    private val lock = SynchronizedObject()
    private val entries = linkedMapOf<K, Entry<V>>()

    init {
        require(capacity > 0) { "Cache capacity must be positive" }
        require(ttlMs > 0L) { "Cache TTL must be positive" }
    }

    fun get(key: K, nowMs: Long): V? = synchronized(lock) {
        val entry = entries.remove(key) ?: return@synchronized null
        if (nowMs - entry.updatedAtMs > ttlMs) return@synchronized null

        entries[key] = entry
        entry.value
    }

    fun put(key: K, value: V, nowMs: Long) = synchronized(lock) {
        entries.remove(key)
        entries[key] = Entry(value, nowMs)
        while (entries.size > capacity) {
            entries.remove(entries.keys.first())
        }
    }

    fun remove(key: K): V? = synchronized(lock) { entries.remove(key)?.value }

    fun clear() = synchronized(lock) { entries.clear() }

    fun size(): Int = synchronized(lock) { entries.size }
}
