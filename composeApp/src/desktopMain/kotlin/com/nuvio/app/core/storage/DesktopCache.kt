package com.nuvio.app.core.storage

import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Comparator

internal object DesktopCache {
    private val versionDirectoryPattern = Regex("[0-9a-f]{24}")

    fun installVersionedFiles(namespace: String, files: Map<String, ByteArray>): Path =
        installVersionedFiles(DesktopStorage.cacheDir, namespace, files)

    internal fun installVersionedFiles(cacheRoot: Path, namespace: String, files: Map<String, ByteArray>): Path {
        require(namespace.isNotBlank())
        require(files.isNotEmpty())
        val version = contentVersion(files)
        val root = cacheRoot.toAbsolutePath().normalize()
        val namespaceDirectory = root.resolve(namespace).normalize()
        require(namespaceDirectory.startsWith(root))
        val directory = namespaceDirectory.resolve(version)
        files.forEach { (relativePath, bytes) ->
            val target = directory.resolve(relativePath).normalize()
            require(target.startsWith(directory))
            writeIfChanged(target, bytes)
        }
        pruneStaleVersions(namespaceDirectory, version)
        return directory
    }

    /** Only content-addressed directories in this exact namespace are disposable. */
    private fun pruneStaleVersions(namespaceDirectory: Path, currentVersion: String) {
        if (!Files.isDirectory(namespaceDirectory, NOFOLLOW_LINKS)) return
        val children = runCatching { Files.newDirectoryStream(namespaceDirectory) }.getOrNull() ?: return
        children.use { entries ->
            entries.forEach { candidate ->
                val name = candidate.fileName?.toString().orEmpty()
                if (name == currentVersion || !versionDirectoryPattern.matches(name) ||
                    !Files.isDirectory(candidate, NOFOLLOW_LINKS)
                ) return@forEach
                deleteTreeBestEffort(candidate)
            }
        }
    }

    /** A failed delete leaves the stale cache for the next install attempt. */
    private fun deleteTreeBestEffort(root: Path) {
        runCatching {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { path ->
                    runCatching { Files.deleteIfExists(path) }
                }
            }
        }
    }

    private fun contentVersion(files: Map<String, ByteArray>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        files.toSortedMap().forEach { (path, bytes) ->
            digest.update(path.toByteArray(Charsets.UTF_8))
            digest.update(0)
            digest.update(bytes)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }.take(24)
    }

    private fun writeIfChanged(target: Path, bytes: ByteArray) {
        if (Files.exists(target) && Files.readAllBytes(target).contentEquals(bytes)) return
        Files.createDirectories(target.parent)
        val pending = Files.createTempFile(target.parent, target.fileName.toString(), ".part")
        try {
            Files.write(pending, bytes)
            runCatching {
                Files.move(
                    pending,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }.getOrElse {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(pending)
        }
    }
}
