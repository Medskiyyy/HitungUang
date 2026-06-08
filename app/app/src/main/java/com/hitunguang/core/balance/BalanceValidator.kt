package com.hitunguang.core.balance

import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.core.database.dao.TransactionDao
import com.hitunguang.core.database.dao.TransferDao
import javax.inject.Inject

/**
 * Memverifikasi dan memperbaiki konsistensi saldo akun.
 * Menghitung ulang saldo dari initialBalance + sum(transactions) ± transfers
 * lalu membandingkan dengan currentBalance yang tersimpan.
 */
class BalanceValidator @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao
) {

    /**
     * Menghitung ulang saldo akun dari nol berdasarkan data transaksi dan transfer.
     * Formula: initialBalance + sum(INCOME) - sum(EXPENSE) - sum(TRANSFER_FEE) + sum(transfer_in) - sum(transfer_out + adminFee)
     */
    suspend fun recalculateBalance(accountId: String): Long {
        val initialBalance = accountDao.getInitialBalance(accountId)
        val incomeSum = transactionDao.sumByTypeAndAccount(accountId, "INCOME")
        val expenseSum = transactionDao.sumByTypeAndAccount(accountId, "EXPENSE")
        val transferFeeSum = transactionDao.sumByTypeAndAccount(accountId, "TRANSFER_FEE")
        val transfersIn = transferDao.sumTransfersIn(accountId)
        val transfersOut = transferDao.sumTransfersOut(accountId)

        return initialBalance + incomeSum - expenseSum - transferFeeSum + transfersIn - transfersOut
    }

    /**
     * Memvalidasi apakah currentBalance yang tersimpan konsisten
     * dengan hasil rekalkulasi dari data transaksi.
     */
    suspend fun validateBalance(accountId: String): BalanceValidationResult {
        val storedBalance = accountDao.getAccountBalance(accountId)
        val calculatedBalance = recalculateBalance(accountId)
        return BalanceValidationResult(
            accountId = accountId,
            storedBalance = storedBalance,
            calculatedBalance = calculatedBalance,
            isConsistent = storedBalance == calculatedBalance
        )
    }

    /**
     * Memperbaiki saldo akun jika terdeteksi inkonsistensi.
     * Mengupdate currentBalance ke nilai hasil rekalkulasi.
     */
    suspend fun repairBalance(accountId: String) {
        val calculatedBalance = recalculateBalance(accountId)
        val storedBalance = accountDao.getAccountBalance(accountId)
        if (storedBalance != calculatedBalance) {
            val difference = calculatedBalance - storedBalance
            accountDao.adjustBalance(accountId, difference, System.currentTimeMillis())
        }
    }
}
