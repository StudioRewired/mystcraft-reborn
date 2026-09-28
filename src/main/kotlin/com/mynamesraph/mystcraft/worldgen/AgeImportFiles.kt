package com.mynamesraph.mystcraft.worldgen

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.CancellationException

/** Filesystem-only work; deliberately has no Minecraft/client/registry dependencies. */
internal object AgeImportFiles {
    const val IMPORT_DIRECTORY = "mystcraft_import"
    private val folderPattern = Regex("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}")
    private val regionPattern = Regex("r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca")

    data class Prepared(val staging: Path, val files: Int, val bytes: Long, val chunks: Int)

    fun validateName(name: String) {
        require(folderPattern.matches(name) && !name.contains("..") && !name.endsWith('.')) {
            "Use a single folder name (1-64 letters, digits, underscores, hyphens or dots); paths and '..' are not allowed."
        }
        val windowsBase = name.substringBefore('.').uppercase(java.util.Locale.ROOT)
        require(windowsBase !in setOf("CON", "PRN", "AUX", "NUL")
            && !Regex("(?:COM|LPT)[0-9]").matches(windowsBase)) { "Reserved filesystem name: $name" }
    }

    /** Refuse symlinks/junctions at every directory boundary, including the import root. */
    fun childDirectory(parent: Path, name: String, create: Boolean): Path {
        val parentReal = parent.toRealPath()
        val child = parentReal.resolve(name)
        if (create) {
            try { Files.createDirectory(child) } catch (_: FileAlreadyExistsException) { }
        }
        require(Files.isDirectory(child, NOFOLLOW_LINKS) && !Files.isSymbolicLink(child)) {
            "Not a real directory (or is a symbolic link): $child"
        }
        require(child.toRealPath() == child.toAbsolutePath().normalize()) {
            "Directory links/junctions are not allowed in the import path: $child"
        }
        return child
    }

    fun source(worldRoot: Path, folderName: String): Path {
        validateName(folderName)
        val area = childDirectory(worldRoot, IMPORT_DIRECTORY, true)
        return childDirectory(area, folderName, false)
    }

    /** Validate, then copy into a private sibling staging directory; never modify the source. */
    fun prepare(source: Path, cancelled: () -> Boolean): Prepared {
        checkCancelled(cancelled)
        require(listOf("dimensions", "datapacks", "playerdata", "advancements", "stats", "DIM-1", "DIM1")
            .none { Files.exists(source.resolve(it), NOFOLLOW_LINKS) }) {
            "Supply a single dimension folder with region/ at its root, not an entire world save."
        }
        var byteCount = 0L
        var fileCount = 0
        // Inspect the entire tree first, not just region files: mod data must not contain escape links.
        visit(source, cancelled) { _, attrs ->
            byteCount = Math.addExact(byteCount, attrs.size())
            fileCount++
        }
        val chunks = validateRegions(source, cancelled)
        require(Files.exists(source.resolve("dimension.json"), NOFOLLOW_LINKS)
            || Files.exists(source.resolve("level.dat"), NOFOLLOW_LINKS)) {
            "Missing dimension metadata. Add dimension.json (the original LevelStem type/generator), " +
                "or a copy of the source world's level.dat. Region files alone cannot restore the original generator."
        }
        require(Files.getFileStore(source.parent).usableSpace >= byteCount) {
            "Not enough free disk space to copy this Age ($byteCount bytes)."
        }
        val staging = Files.createTempDirectory(source.parent, ".staging-")
        try {
            Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    checkCancelled(cancelled)
                    validateEntry(source, dir, attrs, true)
                    val relative = source.relativize(dir)
                    if (dir != source) Files.createDirectory(staging.resolve(relative))
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    checkCancelled(cancelled)
                    validateEntry(source, file, attrs, false)
                    val target = staging.resolve(source.relativize(file))
                    // NOFOLLOW_LINKS on the open prevents a last-moment source symlink from being read.
                    FileChannel.open(file, StandardOpenOption.READ, NOFOLLOW_LINKS).use { input ->
                        FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { output ->
                            val buffer = ByteBuffer.allocate(1024 * 1024)
                            while (true) {
                                checkCancelled(cancelled)
                                buffer.clear()
                                val read = input.read(buffer)
                                if (read < 0) break
                                buffer.flip()
                                while (buffer.hasRemaining()) output.write(buffer)
                            }
                            output.force(false)
                            require(output.size() == attrs.size()) { "Source changed while copying: $file" }
                        }
                    }
                    val after = Files.readAttributes(file, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
                    validateEntry(source, file, after, false)
                    require(after.size() == attrs.size() && after.lastModifiedTime() == attrs.lastModifiedTime()
                        && after.fileKey() == attrs.fileKey()) { "Source changed while copying: $file" }
                    return FileVisitResult.CONTINUE
                }
            })
            // Validate the actual staged copy as well, before it can become a live dimension.
            var copiedFiles = 0
            var copiedBytes = 0L
            visit(staging, cancelled) { _, attrs ->
                copiedFiles++
                copiedBytes = Math.addExact(copiedBytes, attrs.size())
            }
            require(copiedFiles == fileCount && copiedBytes == byteCount) { "Source files changed during copying." }
            require(validateRegions(staging, cancelled) == chunks) { "Region data changed during copying." }
            return Prepared(staging, fileCount, byteCount, chunks)
        } catch (e: Exception) {
            try { deleteStaging(staging) } catch (cleanup: Exception) { e.addSuppressed(cleanup) }
            throw e
        }
    }

    /** Publish with a no-replace move, on the server thread immediately before Infiniverse registration. */
    fun publish(worldRoot: Path, namespace: String, ageNumber: Int, prepared: Prepared): Path {
        require(ageNumber > 0)
        val dimensions = childDirectory(worldRoot, "dimensions", true)
        val ageRoot = childDirectory(dimensions, namespace, true)
        val target = ageRoot.resolve("age_$ageNumber")
        require(!Files.exists(target, NOFOLLOW_LINKS)) { "Age directory already exists: $target" }
        // Do NOT use REPLACE_EXISTING (or ATOMIC_MOVE, whose replacement behavior is implementation-specific).
        return Files.move(prepared.staging, target)
    }

    /** Only temporary paths created by prepare may be cleaned up; never follow links or remove a live Age. */
    fun deleteStaging(path: Path) {
        require(path.fileName.toString().startsWith(".staging-")
            && path.parent.fileName.toString() == IMPORT_DIRECTORY) { "Refusing to delete a non-staging path." }
        if (!Files.exists(path, NOFOLLOW_LINKS)) return
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }
            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc != null) throw exc
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun visit(root: Path, cancelled: () -> Boolean, fileAction: (Path, BasicFileAttributes) -> Unit) {
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                checkCancelled(cancelled)
                validateEntry(root, dir, attrs, true)
                return FileVisitResult.CONTINUE
            }
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                checkCancelled(cancelled)
                validateEntry(root, file, attrs, false)
                fileAction(file, attrs)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun validateEntry(root: Path, path: Path, attrs: BasicFileAttributes, directory: Boolean) {
        require(!attrs.isSymbolicLink && (if (directory) attrs.isDirectory else attrs.isRegularFile)) {
            "Only regular files/directories are allowed; no links or special files: $path"
        }
        val real = path.toRealPath()
        require(real.startsWith(root) && real == path.toAbsolutePath().normalize()) {
            "Path escapes the import folder or traverses a directory link: $path"
        }
    }

    private fun validateRegions(root: Path, cancelled: () -> Boolean): Int {
        val region = childDirectory(root, "region", false)
        var chunks = 0
        for (directory in listOf(region, root.resolve("entities"), root.resolve("poi"))) {
            if (!Files.exists(directory, NOFOLLOW_LINKS)) continue
            require(Files.isDirectory(directory, NOFOLLOW_LINKS)) { "Not a region directory: $directory" }
            Files.newDirectoryStream(directory, "*.mca").use { paths ->
                for (path in paths) {
                    checkCancelled(cancelled)
                    val count = validateRegion(path)
                    if (directory == region) chunks = Math.addExact(chunks, count)
                }
            }
        }
        require(chunks > 0) { "The region/ directory contains no stored chunks to import." }
        return chunks
    }

    /** Validate Anvil locations, bounds, overlap, chunk lengths, codecs and external-chunk references. */
    private fun validateRegion(path: Path): Int {
        val match = regionPattern.matchEntire(path.fileName.toString())
            ?: throw IllegalArgumentException("Invalid Anvil region filename: $path")
        val rx = match.groupValues[1].toIntOrNull() ?: error("Invalid region X: $path")
        val rz = match.groupValues[2].toIntOrNull() ?: error("Invalid region Z: $path")
        FileChannel.open(path, StandardOpenOption.READ, NOFOLLOW_LINKS).use { file ->
            val length = file.size()
            require(length >= 8192 && length % 4096L == 0L) { "Truncated Anvil region: $path" }
            val header = ByteBuffer.allocate(4096).order(ByteOrder.BIG_ENDIAN)
            readFully(file, header, 0)
            val occupied = mutableSetOf<Int>()
            var chunks = 0
            for (slot in 0 until 1024) {
                val entry = header.int
                if (entry == 0) continue
                val offset = entry ushr 8
                val sectors = entry and 255
                require(offset >= 2 && sectors > 0 && (offset.toLong() + sectors) * 4096L <= length) {
                    "Invalid chunk sector range in $path (slot $slot)."
                }
                for (sector in offset until offset + sectors) {
                    require(occupied.add(sector)) { "Overlapping chunk sectors in $path." }
                }
                val chunkHeader = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
                readFully(file, chunkHeader, offset * 4096L)
                val chunkLength = chunkHeader.int
                val compression = chunkHeader.get().toInt() and 255
                require(chunkLength >= 1 && chunkLength <= sectors * 4096 - 4 && (compression and 127) in 1..4) {
                    "Invalid chunk length/compression in $path (slot $slot)."
                }
                if ((compression and 128) != 0) {
                    val external = path.parent.resolve("c.${rx * 32L + slot % 32}.${rz * 32L + slot / 32}.mcc")
                    require(chunkLength == 1 && Files.isRegularFile(external, NOFOLLOW_LINKS) && Files.size(external) > 0) {
                        "Missing/invalid external chunk: $external"
                    }
                } else require(chunkLength > 1) { "Empty chunk payload in $path (slot $slot)." }
                chunks++
            }
            return chunks
        }
    }

    private fun readFully(file: FileChannel, buffer: ByteBuffer, offset: Long) {
        var position = offset
        while (buffer.hasRemaining()) {
            val read = file.read(buffer, position)
            require(read > 0) { "Unexpected end of Anvil region." }
            position += read
        }
        buffer.flip()
    }

    private fun checkCancelled(cancelled: () -> Boolean) {
        if (Thread.currentThread().isInterrupted || cancelled()) throw CancellationException("Age import cancelled.")
    }
}
