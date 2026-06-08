package com.hitunguang.feature.transfer.domain.usecase

import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.feature.transfer.domain.model.Transfer
import com.hitunguang.feature.transfer.domain.repository.TransferRepository
import javax.inject.Inject

class ExecuteTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountDao: AccountDao
) {
    suspend operator fun invoke(transfer: Transfer) {
        if (transfer.fromAccountId == transfer.toAccountId) {
            throw IllegalArgumentException("Akun asal dan tujuan harus berbeda.")
        }
        if (transfer.amount <= 0) {
            throw IllegalArgumentException("Nominal transfer harus lebih dari 0.")
        }
        if (transfer.adminFee < 0) {
            throw IllegalArgumentException("Biaya admin tidak boleh negatif.")
        }

        val totalDeduction = transfer.amount + transfer.adminFee
        val currentBalance = accountDao.getAccountBalance(transfer.fromAccountId)
        if (currentBalance < totalDeduction) {
            throw IllegalStateException("Saldo tidak mencukupi. Saldo saat ini: $currentBalance, dibutuhkan: $totalDeduction.")
        }

        transferRepository.executeTransfer(transfer)
    }
}
