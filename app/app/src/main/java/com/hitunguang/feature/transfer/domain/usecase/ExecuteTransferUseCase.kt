package com.hitunguang.feature.transfer.domain.usecase

import com.hitunguang.feature.transaction.domain.model.UserStateException
import com.hitunguang.feature.transaction.domain.model.UserArgumentException
import com.hitunguang.R
import com.hitunguang.core.common.util.CurrencyFormatter
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
            throw UserArgumentException(R.string.transfer_error_same_account, fallbackMessage = "Dompet asal dan tujuan harus berbeda.")
        }
        if (transfer.amount <= 0) {
            throw UserArgumentException(R.string.transfer_error_amount_positive, fallbackMessage = "Nominal transfer harus lebih dari 0.")
        }
        if (transfer.adminFee < 0) {
            throw UserArgumentException(R.string.transfer_error_fee_negative, fallbackMessage = "Biaya admin tidak boleh negatif.")
        }

        val totalDeduction = transfer.amount + transfer.adminFee
        val currentBalance = accountDao.getAccountBalance(transfer.fromAccountId)
        if (currentBalance < totalDeduction) {
            throw UserStateException(
                R.string.transfer_error_insufficient_balance,
                listOf(CurrencyFormatter.format(currentBalance), CurrencyFormatter.format(totalDeduction)),
                "Saldo nggak cukup. Saldo sekarang: ${CurrencyFormatter.format(currentBalance)}, butuh: ${CurrencyFormatter.format(totalDeduction)}."
            )
        }

        transferRepository.executeTransfer(transfer)
    }
}
