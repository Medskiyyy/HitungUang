package com.hitunguang.feature.transfer.domain.repository

import com.hitunguang.feature.transfer.domain.model.Transfer
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun getAllTransfers(): Flow<List<Transfer>>
    fun getTransferById(id: String): Flow<Transfer?>
    suspend fun executeTransfer(transfer: Transfer)
    suspend fun revertTransfer(transfer: Transfer)
}
