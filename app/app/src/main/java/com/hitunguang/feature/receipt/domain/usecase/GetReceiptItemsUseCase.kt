package com.hitunguang.feature.receipt.domain.usecase

import com.hitunguang.feature.receipt.domain.model.ReceiptItem
import com.hitunguang.feature.receipt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReceiptItemsUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository
) {
    operator fun invoke(receiptId: String): Flow<List<ReceiptItem>> {
        return receiptRepository.getReceiptItems(receiptId)
    }
}
