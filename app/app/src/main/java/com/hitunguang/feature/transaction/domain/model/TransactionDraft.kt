package com.hitunguang.feature.transaction.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDraft(
    val transactionType: String? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val title: String? = null,
    val note: String? = null,
    val amount: Long? = null,
    val updatedAt: Long
)
