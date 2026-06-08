package com.hitunguang.feature.account.domain.usecase

import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account, replacementAccountId: String? = null) {
        val txCount = accountRepository.getTransactionCountForAccount(account.id)
        val transferCount = accountRepository.getTransferCountForAccount(account.id)
        
        if (txCount > 0 || transferCount > 0) {
            if (replacementAccountId == null) {
                throw IllegalStateException("Akun memiliki transaksi/transfer dan membutuhkan akun pengganti.")
            }
            // Migrate data first
            accountRepository.transferAccountData(account.id, replacementAccountId, account.currentBalance)
        }
        
        // Delete the account
        accountRepository.deleteAccount(account)
    }
}
