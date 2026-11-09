package com.hitunguang.feature.recurring.domain.model

data class RecurringTransaction(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val transactionType: String,
    val title: String,
    val note: String?,
    val amount: Long,
    val recurrenceRule: RecurrenceRule,
    val nextRunAt: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
