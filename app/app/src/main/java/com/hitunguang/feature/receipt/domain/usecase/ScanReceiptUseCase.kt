package com.hitunguang.feature.receipt.domain.usecase

import android.net.Uri
import com.hitunguang.core.ocr.OcrManager
import javax.inject.Inject

class ScanReceiptUseCase @Inject constructor(
    private val ocrManager: OcrManager
) {
    suspend operator fun invoke(imageUri: Uri): Result<String> {
        return ocrManager.recognizeText(imageUri)
    }
}
