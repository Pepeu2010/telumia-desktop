package com.nuvio.app.core.cache

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** Synchronous bounded LRU for values read from non-suspending UI APIs. */
internal class SynchronizedBoundedLruCache<K : Any, V : Any>(private val capacity: Int) {
    private val lock = SynchronizedObject()
    private val entries = linkedMapOf<K, V>()

    init {
        require(capacity > 0) { "Cache capacity must be positive" }
    }

    fun get(key: K): V? = synchronized(lock) {
        entries.remove(key)?.also { value -> entries[key] = value }
    }

    fun put(key: K, value: V) = synchronized(lock) {
        entries.remove(key)
        entries[key] = value
        while (entries.size > capacity) {
            entries.remove(entries.keys.first())
        }
    }

    fun clear() = synchronized(lock) { entries.clear() }

    fun size(): Int = synchronized(lock) { entries.size }
}
