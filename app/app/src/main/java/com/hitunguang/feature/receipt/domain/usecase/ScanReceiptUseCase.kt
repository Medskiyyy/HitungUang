package com.hitunguang.feature.receipt.domain.usecase

import android.net.Uri
import com.hitunguang.feature.receipt.domain.model.ScanReceiptResult
import com.hitunguang.core.ocr.OcrManager
import com.hitunguang.core.ocr.OcrResult
import javax.inject.Inject

class ScanReceiptUseCase @Inject constructor(
    private val ocrManager: OcrManager
) {
    suspend operator fun invoke(imageUri: Uri): Result<ScanReceiptResult> {
        return ocrManager.recognizeText(imageUri).map { ocrResult ->
            ScanReceiptResult(
                text = ocrResult.text,
                averageConfidence = ocrResult.averageConfidence
            )
        }
    }
}

