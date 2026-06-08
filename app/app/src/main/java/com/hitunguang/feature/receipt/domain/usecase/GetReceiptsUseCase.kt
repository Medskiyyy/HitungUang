package com.hitunguang.feature.receipt.domain.usecase

import com.hitunguang.feature.receipt.domain.model.Receipt
import com.hitunguang.feature.receipt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReceiptsUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository
) {
    operator fun invoke(): Flow<List<Receipt>> {
        return receiptRepository.getAllReceipts()
    }
}
