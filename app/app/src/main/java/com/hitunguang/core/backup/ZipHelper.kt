package com.hitunguang.core.backup

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipHelper {

    /**
     * Adds a file to an open ZipOutputStream with the given entry name.
     */
    fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    /**
     * Adds all files in a directory to an open ZipOutputStream, preserving relative paths.
     * @param zip the open ZipOutputStream
     * @param directory the source directory
     * @param zipPrefix the prefix for entries, e.g. "attachments/"
     */
    fun addDirectoryToZip(zip: ZipOutputStream, directory: File, zipPrefix: String) {
        if (!directory.exists() || !directory.isDirectory) return
        directory.walkTopDown().filter { it.isFile }.forEach { file ->
            val relativePath = file.relativeTo(directory).path.replace('\\', '/')
            addFileToZip(zip, file, "$zipPrefix$relativePath")
        }
    }

    /**
     * Extracts all entries from a ZipInputStream to the given output directory.
     * Skips entries that would extract outside the output directory (zip-slip protection).
     */
    fun extractZip(inputStream: InputStream, outputDir: File) {
        outputDir.mkdirs()
        ZipInputStream(inputStream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val target = File(outputDir, entry.name)
                // Zip-slip protection: ensure target is inside outputDir
                val canonicalOutput = outputDir.canonicalPath
                if (!target.canonicalPath.startsWith(canonicalOutput + File.separator)) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    /**
     * Returns true if a ZipInputStream (from inputStream) contains an entry matching entryName.
     */
    fun containsEntry(inputStream: InputStream, entryName: String): Boolean {
        ZipInputStream(inputStream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == entryName) return true
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return false
    }
}
