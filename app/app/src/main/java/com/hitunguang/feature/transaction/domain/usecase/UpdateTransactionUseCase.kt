package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.core.balance.BalanceCalculator
import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import javax.inject.Inject
import com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountDao: AccountDao,
    private val checkBudgetThresholdUseCase: CheckBudgetThresholdUseCase? = null
) {
    suspend operator fun invoke(oldTransaction: Transaction, newTransaction: Transaction) {
        if (newTransaction.title.trim().isEmpty()) {
            throw IllegalArgumentException("Judulnya belum diisi.")
        }
        if (newTransaction.amount <= 0) {
            throw IllegalArgumentException("Nominal transaksi harus lebih dari 0.")
        }

        val oldBalanceDiff = BalanceCalculator.calculateDifference(
            oldTransaction.transactionType, oldTransaction.amount
        )
        val newBalanceDiff = BalanceCalculator.calculateDifference(
            newTransaction.transactionType, newTransaction.amount
        )

        // Validasi saldo negatif setelah update
        if (oldTransaction.accountId != newTransaction.accountId) {
            // Akun berubah: cek saldo akun baru
            if (newBalanceDiff < 0) {
                val newAccountBalance = accountDao.getAccountBalance(newTransaction.accountId)
                if (newAccountBalance + newBalanceDiff < 0) {
                    throw IllegalStateException("Saldo dompet tujuan nggak cukup.")
                }
            }
        } else {
            // Akun tetap: cek dampak net difference
            val netDifference = newBalanceDiff - oldBalanceDiff
            if (netDifference < 0) {
                val currentBalance = accountDao.getAccountBalance(newTransaction.accountId)
                if (currentBalance + netDifference < 0) {
                    throw IllegalStateException("Saldo nggak cukup buat perubahan ini.")
                }
            }
        }

        transactionRepository.updateTransaction(
            oldTransaction = oldTransaction,
            newTransaction = newTransaction,
            oldBalanceDiff = oldBalanceDiff,
            newBalanceDiff = newBalanceDiff
        )

        // Re-check budgets touched by the old and the new version of an expense
        val affected = listOf(oldTransaction, newTransaction)
            .filter { it.transactionType == "EXPENSE" }
            .map { it.categoryId to it.transactionDate }
            .distinct()
        for ((categoryId, transactionDate) in affected) {
            try {
                checkBudgetThresholdUseCase?.invoke(categoryId, transactionDate)
            } catch (e: Exception) {
                // Ignore background trigger errors in unit tests or runtime
            }
        }
    }
}
