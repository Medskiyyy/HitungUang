package com.hitunguang.core.filemanager

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val receiptsDir: File by lazy {
        File(context.filesDir, "receipts").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    suspend fun saveReceiptImage(sourceUri: Uri): File = withContext(Dispatchers.IO) {
        val uniqueName = "${UUID.randomUUID()}.jpg"
        val outputFile = File(receiptsDir, uniqueName)

        val decodedBitmap = BitmapDecoder.decodeUpright(context, sourceUri)
        val finalBitmap = resizeBitmapIfNeeded(decodedBitmap, 1280)

        FileOutputStream(outputFile).use { outputStream ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        }
        finalBitmap.recycle()

        outputFile
    }

    /**
     * Copies a picked image into the cache. Photo-picker URIs are only readable for a
     * limited time, while the review screen may save the receipt much later.
     */
    suspend fun copyToCache(sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        val file = File.createTempFile(PICKED_PHOTO_PREFIX, ".jpg", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("Fotonya nggak bisa dibuka. Coba pilih foto lain.")
            input.use { inputStream ->
                FileOutputStream(file).use { inputStream.copyTo(it) }
            }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        Uri.fromFile(file)
    }

    /** Deletes camera / picked temp files left behind for more than a day. */
    suspend fun deleteStaleTempFiles() = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - STALE_TEMP_FILE_AGE_MILLIS
        context.cacheDir.listFiles()
            ?.filter { isTempPhotoName(it.name) && it.lastModified() < threshold }
            ?.forEach { it.delete() }
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val aspectRatio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            maxDimension to (maxDimension / aspectRatio).toInt()
        } else {
            (maxDimension * aspectRatio).toInt() to maxDimension
        }
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }

    /** Deletes [sourceUri] when it points at one of this app's camera / picked temp files. */
    fun deleteTempFileIfNeeded(sourceUri: Uri) {
        try {
            if (sourceUri.scheme == "file") {
                val file = sourceUri.path?.let { File(it) }
                if (file != null && file.exists() && file.parentFile?.absolutePath == context.cacheDir.absolutePath) {
                    if (isTempPhotoName(file.name)) {
                        file.delete()
                    }
                }
            } else if (sourceUri.scheme == "content" && sourceUri.authority == "com.hitunguang.fileprovider") {
                val fileName = sourceUri.lastPathSegment
                if (fileName != null && isTempPhotoName(fileName)) {
                    val fileInCache = File(context.cacheDir, fileName)
                    if (fileInCache.exists()) {
                        fileInCache.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore failure to ensure the main save operation completes successfully
        }
    }

    private fun isTempPhotoName(name: String): Boolean =
        name.startsWith(CAMERA_PHOTO_PREFIX) || name.startsWith(PICKED_PHOTO_PREFIX)

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    private companion object {
        const val CAMERA_PHOTO_PREFIX = "camera_photo_"
        const val PICKED_PHOTO_PREFIX = "picked_photo_"
        const val STALE_TEMP_FILE_AGE_MILLIS = 24 * 60 * 60 * 1000L
    }
}
