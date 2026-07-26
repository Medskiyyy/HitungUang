package com.hitunguang.core.ocr

import android.net.Uri

/**
 * @param averageConfidence null when the recognizer exposes no confidence data.
 *        ML Kit's on-device latin recognizer does not, so callers must derive scan
 *        quality from the text itself instead of assuming a perfect score.
 */
data class OcrResult(
    val text: String,
    val averageConfidence: Float? = null
)

interface OcrManager {
    suspend fun recognizeText(imageUri: Uri): Result<OcrResult>
}
