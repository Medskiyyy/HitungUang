package com.hitunguang.feature.account.domain.usecase

import com.hitunguang.feature.account.domain.repository.AccountRepository
import javax.inject.Inject

class TransferAccountDataUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(oldAccountId: String, newAccountId: String, balanceToTransfer: Long) {
        accountRepository.transferAccountData(oldAccountId, newAccountId, balanceToTransfer)
    }
}
