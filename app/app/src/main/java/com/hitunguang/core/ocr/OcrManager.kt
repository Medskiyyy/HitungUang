package com.hitunguang.core.ocr

import android.net.Uri

interface OcrManager {
    suspend fun recognizeText(imageUri: Uri): Result<String>
}
