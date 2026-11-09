package com.hitunguang.feature.recyclebin.domain.model

import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import androidx.annotation.StringRes

data class RecycleBinItem(
    val id: String,
    val entityType: String,
    val entityId: String,
    val deletedAt: Long,
    val expireAt: Long,
    val title: String,
    val subtitle: String,
    @StringRes val titleRes: Int? = null,
    @StringRes val subtitleRes: Int? = null,
    @StringRes val subtitleArgRes: Int? = null,
    val amountText: String?,
    val isExpense: Boolean,
    val transactionDetails: TransactionWithDetails? = null,
    val isDefault: Boolean = false
)
