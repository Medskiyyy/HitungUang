package com.hitunguang.feature.recyclebin.domain.usecase

import com.hitunguang.feature.recyclebin.domain.repository.RecycleBinRepository
import javax.inject.Inject

class PermanentDeleteTransactionUseCase @Inject constructor(
    private val repository: RecycleBinRepository
) {
    suspend operator fun invoke(transactionId: String) {
        repository.permanentDeleteTransaction(transactionId)
    }
}
