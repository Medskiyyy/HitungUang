package com.hitunguang.core.filemanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
class AttachmentFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val attachmentsDir: File by lazy {
        File(context.filesDir, "attachments").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    suspend fun saveAttachment(sourceUri: Uri): File = withContext(Dispatchers.IO) {
        val uniqueName = "${UUID.randomUUID()}.jpg"
        val outputFile = File(attachmentsDir, uniqueName)

        val decodedBitmap = decodeSampledBitmapFromUri(sourceUri, 1280, 1280)
        val finalBitmap = resizeBitmapIfNeeded(decodedBitmap, 1280)

        FileOutputStream(outputFile).use { outputStream ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        }
        finalBitmap.recycle()

        deleteTempFileIfNeeded(sourceUri)

        outputFile
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri).use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        
        return context.contentResolver.openInputStream(uri).use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        } ?: throw IllegalArgumentException("Could not decode image from URI")
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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

    private fun deleteTempFileIfNeeded(sourceUri: Uri) {
        try {
            if (sourceUri.scheme == "file") {
                val file = sourceUri.path?.let { File(it) }
                if (file != null && file.exists() && file.parentFile?.absolutePath == context.cacheDir.absolutePath) {
                    if (file.name.startsWith("camera_photo_")) {
                        file.delete()
                    }
                }
            } else if (sourceUri.scheme == "content" && sourceUri.authority == "com.hitunguang.fileprovider") {
                val fileName = sourceUri.lastPathSegment
                if (fileName != null && fileName.startsWith("camera_photo_")) {
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

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
