package com.hitunguang.feature.recurring.presentation

import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.recurring.domain.model.RecurringTransaction

data class RecurringTransactionItem(
    val recurring: RecurringTransaction,
    val accountName: String?,
    val categoryName: String?
)

data class RecurringTransactionUiState(
    val items: List<RecurringTransactionItem> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val showFormDialog: Boolean = false,
    val recurringToEdit: RecurringTransaction? = null,
    val error: String? = null,
    val isLoading: Boolean = false
)
