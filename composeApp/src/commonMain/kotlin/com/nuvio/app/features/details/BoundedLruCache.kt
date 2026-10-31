package com.nuvio.app.features.details

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** A small thread-safe, access-ordered cache for regenerable detail metadata. */
internal class BoundedLruCache<K : Any, V : Any>(
    private val maxEntries: Int,
) {
    private val lock = SynchronizedObject()
    private val entries = mutableMapOf<K, V>()
    private val accessOrder = mutableListOf<K>()

    init {
        require(maxEntries > 0)
    }

    operator fun get(key: K): V? = synchronized(lock) {
        val value = entries[key] ?: return@synchronized null
        accessOrder.remove(key)
        accessOrder.add(key)
        value
    }

    operator fun set(key: K, value: V) = synchronized(lock) {
        entries[key] = value
        accessOrder.remove(key)
        accessOrder.add(key)
        while (entries.size > maxEntries) {
            val leastRecentlyUsed = accessOrder.removeAt(0)
            entries.remove(leastRecentlyUsed)
        }
    }

    fun putIfPresent(key: K, value: V?) {
        if (value != null) this[key] = value
    }

    fun clear() = synchronized(lock) {
        entries.clear()
        accessOrder.clear()
    }

    internal fun size(): Int = synchronized(lock) {
        entries.size
    }
}
