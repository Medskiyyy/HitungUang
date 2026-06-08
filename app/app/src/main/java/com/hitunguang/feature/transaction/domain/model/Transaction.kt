package com.hitunguang.feature.transaction.domain.model

data class Transaction(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val receiptId: String?,
    val transactionType: String,
    val title: String,
    val note: String?,
    val amount: Long,
    val transactionDate: Long,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
