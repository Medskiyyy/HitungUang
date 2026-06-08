package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.core.balance.BalanceCalculator
import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountDao: AccountDao
) {
    suspend operator fun invoke(oldTransaction: Transaction, newTransaction: Transaction) {
        if (newTransaction.title.trim().isEmpty()) {
            throw IllegalArgumentException("Judul transaksi tidak boleh kosong.")
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
                    throw IllegalStateException("Saldo akun tujuan tidak mencukupi.")
                }
            }
        } else {
            // Akun tetap: cek dampak net difference
            val netDifference = newBalanceDiff - oldBalanceDiff
            if (netDifference < 0) {
                val currentBalance = accountDao.getAccountBalance(newTransaction.accountId)
                if (currentBalance + netDifference < 0) {
                    throw IllegalStateException("Saldo tidak mencukupi untuk perubahan ini.")
                }
            }
        }

        transactionRepository.updateTransaction(
            oldTransaction = oldTransaction,
            newTransaction = newTransaction,
            oldBalanceDiff = oldBalanceDiff,
            newBalanceDiff = newBalanceDiff
        )
    }
}
