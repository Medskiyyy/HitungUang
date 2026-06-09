package com.hitunguang.feature.transfer.domain.usecase

import com.hitunguang.feature.transfer.domain.model.Transfer
import com.hitunguang.feature.transfer.domain.repository.TransferRepository
import javax.inject.Inject

class RevertTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {
    suspend operator fun invoke(transfer: Transfer) {
        transferRepository.revertTransfer(transfer)
    }
}
