package com.hitunguang.feature.recyclebin.domain.model

import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails

data class RecycleBinItem(
    val id: String,
    val entityType: String,
    val entityId: String,
    val deletedAt: Long,
    val expireAt: Long,
    val title: String,
    val subtitle: String,
    val amountText: String?,
    val isExpense: Boolean,
    val transactionDetails: TransactionWithDetails? = null,
    val isDefault: Boolean = false
)
