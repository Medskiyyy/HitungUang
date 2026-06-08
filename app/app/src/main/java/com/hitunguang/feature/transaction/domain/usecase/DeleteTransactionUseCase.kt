package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.core.balance.BalanceCalculator
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        val balanceDifference = BalanceCalculator.calculateReversal(
            transaction.transactionType, transaction.amount
        )
        transactionRepository.deleteTransaction(transaction, balanceDifference)
    }
}
