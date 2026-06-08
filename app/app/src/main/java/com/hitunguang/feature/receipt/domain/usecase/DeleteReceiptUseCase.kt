package com.hitunguang.feature.receipt.domain.usecase

import com.hitunguang.feature.receipt.domain.model.Receipt
import com.hitunguang.feature.receipt.domain.repository.ReceiptRepository
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import com.hitunguang.feature.transaction.domain.usecase.DeleteTransactionUseCase
import javax.inject.Inject

class DeleteReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val transactionRepository: TransactionRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) {
    suspend operator fun invoke(receipt: Receipt) {
        val transaction = transactionRepository.getTransactionByReceiptId(receipt.id)
        if (transaction != null) {
            deleteTransactionUseCase(transaction)
        }
        receiptRepository.deleteReceipt(receipt)
    }
}
