package com.hitunguang.core.ocr

import android.content.Context
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
            // and already rotated upright.
            val bitmap = withContext(Dispatchers.IO) { BitmapDecoder.decodeUpright(context, imageUri) }
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

    private suspend fun processImage(image: InputImage): OcrResult = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    continuation.resume(OcrResult(text = visionText.text))
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
}
