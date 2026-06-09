package com.hitunguang.core.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : OcrManager {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(imageUri: Uri): Result<String> {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            val text = processImage(image)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processImage(image: InputImage): String = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    continuation.resume(visionText.text)
                }
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
    }
}
