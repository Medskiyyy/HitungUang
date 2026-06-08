package com.hitunguang.feature.recyclebin.domain.model

import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails

data class RecycleBinItem(
    val id: String,
    val entityType: String,
    val entityId: String,
    val deletedAt: Long,
    val expireAt: Long,
    val transactionDetails: TransactionWithDetails? = null
)
