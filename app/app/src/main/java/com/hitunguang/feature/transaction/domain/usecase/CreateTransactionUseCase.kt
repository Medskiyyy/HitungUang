package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.core.balance.BalanceCalculator
import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import javax.inject.Inject
import com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase

class CreateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountDao: AccountDao,
    private val checkBudgetThresholdUseCase: CheckBudgetThresholdUseCase
) {
    suspend operator fun invoke(transaction: Transaction) {
        if (transaction.title.trim().isEmpty()) {
            throw IllegalArgumentException("Judul transaksi tidak boleh kosong.")
        }
        if (transaction.amount <= 0) {
            throw IllegalArgumentException("Nominal transaksi harus lebih dari 0.")
        }

        val balanceDifference = BalanceCalculator.calculateDifference(
            transaction.transactionType, transaction.amount
        )

        // Validasi saldo negatif: cek apakah saldo cukup sebelum mengurangi
        if (balanceDifference < 0) {
            val currentBalance = accountDao.getAccountBalance(transaction.accountId)
            if (currentBalance + balanceDifference < 0) {
                throw IllegalStateException("Saldo tidak mencukupi. Saldo saat ini: $currentBalance, dibutuhkan: ${-balanceDifference}.")
            }
        }

        transactionRepository.insertTransaction(transaction, balanceDifference)

        if (transaction.transactionType == "EXPENSE") {
            try {
                checkBudgetThresholdUseCase(transaction.categoryId)
            } catch (e: Exception) {
                // Ignore background trigger errors in unit tests or runtime
            }
        }
    }
}
