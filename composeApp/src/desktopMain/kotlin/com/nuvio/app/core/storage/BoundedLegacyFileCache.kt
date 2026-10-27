package com.nuvio.app.core.storage

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.security.MessageDigest

/** One byte/file budget shared by a hashed cache and recognized files from its legacy directory. */
internal class BoundedLegacyFileCache(
    directory: Path,
    legacyDirectory: Path,
    private val quotaBytes: () -> Long,
    private val maxFiles: Int = 64,
    private val maxEntryBytes: Long = 8L * MIB,
    private val canWrite: (Long) -> Boolean = { true },
    private val clock: () -> Long = System::currentTimeMillis,
    private val legacyFileName: (String) -> String? = { key ->
        val name = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
        name.takeIf { it.isNotEmpty() && it.length <= 200 }?.let { "$it.png" }
    },
    private val legacyName: Regex = Regex("[A-Za-z0-9._-]{1,200}\\.png"),
) {
    internal data class Usage(val bytes: Long, val files: Int)

    private val directory = directory.toAbsolutePath().normalize()
    private val legacyDirectory = legacyDirectory.toAbsolutePath().normalize()
    private val lock = Any()
    private val ownedName = Regex("[a-f0-9]{64}\\.cache")
    private var lastTrimAtMillis: Long? = null
    private var lastTrimQuota: Long? = null

    fun get(key: String): ByteArray? = getFile(key)?.let { runCatching { Files.readAllBytes(it.toPath()) }.getOrNull() }

    fun getFile(key: String): File? = synchronized(lock) {
        if (!validKey(key)) return@synchronized null
        val quota = quotaBytes().coerceAtLeast(0)
        val owned = ownedFile(key)
        if (isValidFile(owned, directory, ownedName)) {
            val size = runCatching { Files.size(owned) }.getOrNull() ?: return@synchronized null
            if (size > maxEntryBytes) {
                runCatching { Files.deleteIfExists(owned) }
                trimIfNeeded(quota)
                return@synchronized null
            }
            touch(owned)
            trimIfNeeded(quota, keep = owned)
            return@synchronized owned.takeIf { isValidFile(it, directory, ownedName) }?.toFile()
        }

        val legacy = legacyFile(key) ?: return@synchronized null
        if (!isValidFile(legacy, legacyDirectory, legacyName)) {
            trimIfNeeded(quota)
            return@synchronized null
        }
        val size = runCatching { Files.size(legacy) }.getOrNull() ?: return@synchronized null
        if (size > maxEntryBytes) {
            runCatching { Files.deleteIfExists(legacy) }
            trimIfNeeded(quota)
            return@synchronized null
        }
        touch(legacy)
        trimIfNeeded(quota, keep = legacy)
        if (!isValidFile(legacy, legacyDirectory, legacyName)) return@synchronized null

        if (runCatching { canWrite(size) }.getOrDefault(false)) {
            val bytes = runCatching { Files.readAllBytes(legacy) }.getOrNull()
            if (bytes != null && bytes.size.toLong() == size) {
                writeEntry(key, bytes, quota, legacy)?.let { return@synchronized it }
            }
        }
        legacy.takeIf { isValidFile(it, legacyDirectory, legacyName) }?.toFile()
    }

    fun put(key: String, bytes: ByteArray): File? = synchronized(lock) {
        if (!validKey(key) || bytes.isEmpty() || bytes.size.toLong() > maxEntryBytes) return@synchronized null
        val quota = quotaBytes().coerceAtLeast(0)
        if (bytes.size.toLong() > quota || !runCatching { canWrite(bytes.size.toLong()) }.getOrDefault(false)) {
            return@synchronized null
        }
        writeEntry(key, bytes, quota, legacyFile(key))
    }

    internal fun usage(): Usage = synchronized(lock) {
        val entries = listEntries()
        Usage(entries.sumSaturated { it.bytes }, entries.size)
    }

    private data class Entry(val path: Path, val bytes: Long, val modifiedAt: Long)

    private fun writeEntry(key: String, bytes: ByteArray, quota: Long, replacedLegacy: Path?): File? {
        val target = ownedFile(key)
        if (!makeRoom(quota, target, replacedLegacy, bytes.size.toLong())) return null
        if (!writeAtomically(target, bytes)) return null
        if (replacedLegacy != null && replacedLegacy != target &&
            isValidFile(replacedLegacy, legacyDirectory, legacyName)
        ) {
            runCatching { Files.deleteIfExists(replacedLegacy) }
        }
        trim(quota, keep = target)
        lastTrimAtMillis = clock()
        lastTrimQuota = quota
        return target.takeIf { isValidFile(it, directory, ownedName) }?.toFile()
    }

    private fun makeRoom(quota: Long, target: Path, replacedLegacy: Path?, newBytes: Long): Boolean {
        if (quota <= 0 || newBytes > quota || maxFiles <= 0) return false
        val entries = listEntries()
        val targetEntry = entries.firstOrNull { it.path == target }
        val legacyEntry = replacedLegacy?.let { path -> entries.firstOrNull { it.path == path } }
        var projectedBytes = entries.sumSaturated { it.bytes }
        var projectedFiles = entries.size
        if (targetEntry != null) {
            projectedBytes = (projectedBytes - targetEntry.bytes).coerceAtLeast(0)
            projectedFiles--
        }
        if (legacyEntry != null && legacyEntry.path != target) {
            projectedBytes = (projectedBytes - legacyEntry.bytes).coerceAtLeast(0)
            projectedFiles--
        }
        projectedBytes = addSaturated(projectedBytes, newBytes)
        projectedFiles++

        for (entry in entries.sortedBy { it.modifiedAt }) {
            if (projectedBytes <= quota && projectedFiles <= maxFiles) break
            if (entry.path == target || entry.path == replacedLegacy) continue
            if (runCatching { Files.deleteIfExists(entry.path) }.getOrDefault(false)) {
                projectedBytes = (projectedBytes - entry.bytes).coerceAtLeast(0)
                projectedFiles--
            }
        }
        return projectedBytes <= quota && projectedFiles <= maxFiles
    }

    private fun trim(quota: Long, keep: Path? = null) {
        val entries = listEntries()
        var bytes = entries.sumSaturated { it.bytes }
        var files = entries.size
        for (entry in entries.sortedBy { it.modifiedAt }) {
            if (entry.bytes <= 0 || entry.bytes > maxEntryBytes) {
                if (entry.path != keep && runCatching { Files.deleteIfExists(entry.path) }.getOrDefault(false)) {
                    bytes = (bytes - entry.bytes).coerceAtLeast(0)
                    files--
                }
                continue
            }
            if (bytes <= quota && files <= maxFiles) break
            if (entry.path == keep) continue
            if (runCatching { Files.deleteIfExists(entry.path) }.getOrDefault(false)) {
                bytes = (bytes - entry.bytes).coerceAtLeast(0)
                files--
            }
        }
    }

    /** Disk-wide cleanup is rate-limited on reads; every new write still checks the full budget. */
    private fun trimIfNeeded(quota: Long, keep: Path? = null) {
        val now = clock()
        val previousTrim = lastTrimAtMillis
        if (lastTrimQuota != quota || previousTrim == null || now < previousTrim ||
            now - previousTrim >= READ_TRIM_INTERVAL_MILLIS
        ) {
            trim(quota, keep)
            lastTrimAtMillis = now
            lastTrimQuota = quota
        }
    }

    private fun listEntries(): List<Entry> = buildList {
        addEntries(directory, ownedName)
        addEntries(legacyDirectory, legacyName)
    }

    private fun MutableList<Entry>.addEntries(root: Path, ownedNames: Regex) {
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, NOFOLLOW_LINKS)) return
        val realRoot = runCatching { root.toRealPath() }.getOrNull() ?: return
        runCatching {
            Files.newDirectoryStream(root).use { entries ->
                for (path in entries) {
                    if (!ownedNames.matches(path.fileName.toString())) continue
                    val attributes = runCatching {
                        Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
                    }.getOrNull() ?: continue
                    if (!attributes.isRegularFile || attributes.isSymbolicLink) continue
                    if (!runCatching { path.toRealPath().startsWith(realRoot) }.getOrDefault(false)) continue
                    add(Entry(path, attributes.size().coerceAtLeast(0), attributes.lastModifiedTime().toMillis()))
                }
            }
        }
    }

    private fun isValidFile(path: Path, root: Path, names: Regex): Boolean = runCatching {
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, NOFOLLOW_LINKS) ||
            !names.matches(path.fileName.toString())
        ) return@runCatching false
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
        attributes.isRegularFile && !attributes.isSymbolicLink &&
            attributes.size() in 1..maxEntryBytes && path.toRealPath().startsWith(root.toRealPath())
    }.getOrDefault(false)

    private fun touch(path: Path) {
        runCatching { Files.setLastModifiedTime(path, FileTime.fromMillis(clock())) }
    }

    private fun writeAtomically(target: Path, bytes: ByteArray): Boolean = runCatching {
        Files.createDirectories(directory)
        require(!Files.isSymbolicLink(directory))
        val pending = Files.createTempFile(directory, "cache-entry-", ".part")
        try {
            Files.write(pending, bytes)
            runCatching { Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
                .getOrElse { Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING) }
            Files.setLastModifiedTime(target, FileTime.fromMillis(clock()))
            isValidFile(target, directory, ownedName) && Files.size(target) == bytes.size.toLong()
        } finally {
            Files.deleteIfExists(pending)
        }
    }.getOrDefault(false)

    private fun ownedFile(key: String): Path = directory.resolve("${sha256(key)}.cache")

    private fun legacyFile(key: String): Path? = legacyFileName(key)
        ?.takeIf { legacyName.matches(it) }
        ?.let(legacyDirectory::resolve)

    private fun validKey(key: String): Boolean = key.isNotBlank() && key.length <= 8192

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private inline fun <T> Iterable<T>.sumSaturated(size: (T) -> Long): Long =
        fold(0L) { total, value -> addSaturated(total, size(value).coerceAtLeast(0)) }

    private fun addSaturated(first: Long, second: Long): Long =
        if (first > Long.MAX_VALUE - second) Long.MAX_VALUE else first + second

    private companion object {
        const val READ_TRIM_INTERVAL_MILLIS = 15_000L
    }
}
