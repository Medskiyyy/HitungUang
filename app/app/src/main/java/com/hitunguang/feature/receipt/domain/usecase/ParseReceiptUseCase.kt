package com.hitunguang.feature.receipt.domain.usecase

import com.hitunguang.core.ocr.ParsedReceipt
import com.hitunguang.core.ocr.ReceiptParser
import javax.inject.Inject

class ParseReceiptUseCase @Inject constructor() {
    operator fun invoke(rawText: String?): ParsedReceipt {
        return ReceiptParser.parse(rawText)
    }
}
