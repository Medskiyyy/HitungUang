package com.hitunguang.feature.recyclebin.domain.repository

import com.hitunguang.feature.recyclebin.domain.model.RecycleBinItem
import kotlinx.coroutines.flow.Flow

interface RecycleBinRepository {
    fun getDeletedItems(): Flow<List<RecycleBinItem>>
    suspend fun restoreTransaction(transactionId: String)
    suspend fun permanentDeleteTransaction(transactionId: String)
    suspend fun cleanupExpiredItems(currentTime: Long)
}
