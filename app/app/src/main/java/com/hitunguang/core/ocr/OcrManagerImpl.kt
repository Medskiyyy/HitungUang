package com.hitunguang.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.hitunguang.core.filemanager.BitmapDecoder
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : OcrManager, Closeable {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognizeText(imageUri: Uri): Result<OcrResult> {
        return try {
            // A full-resolution camera photo can exhaust the heap: decode it downscaled
            // and already rotated upright. Small downloaded images are upscaled so digits
            // stay above ML Kit's comfortable recognition height.
            val decoded = withContext(Dispatchers.IO) { BitmapDecoder.decodeUpright(context, imageUri) }
            val bitmap = upscaleIfSmall(decoded)
            val result = try {
                processImage(InputImage.fromBitmap(bitmap, 0))
            } finally {
                bitmap.recycle()
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun upscaleIfSmall(bitmap: Bitmap): Bitmap {
        val minSide = minOf(bitmap.width, bitmap.height)
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (minSide >= TARGET_MIN_SIDE || maxSide >= MAX_UPSCALED_SIDE || minSide <= 0 || maxSide <= 0) {
            return bitmap
        }
        val targetScale = TARGET_MIN_SIDE.toFloat() / minSide
        val maxSideScale = MAX_UPSCALED_SIDE.toFloat() / maxSide
        val scale = minOf(targetScale, maxSideScale, MAX_UPSCALE_FACTOR)
        if (scale <= 1.05f) {
            return bitmap
        }
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(bitmap.width)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(bitmap.height)
        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private suspend fun processImage(image: InputImage): OcrResult = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    continuation.resume(OcrResult(text = VisionTextFormatter.format(visionText)))
                }
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
    }

    override fun close() {
        recognizer.close()
    }

    private companion object {
        const val TARGET_MIN_SIDE = 960
        const val MAX_UPSCALED_SIDE = 2000
        const val MAX_UPSCALE_FACTOR = 3.5f
    }
}
