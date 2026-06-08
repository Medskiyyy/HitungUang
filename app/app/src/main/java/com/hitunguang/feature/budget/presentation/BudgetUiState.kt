package com.hitunguang.feature.budget.presentation

import com.hitunguang.feature.budget.domain.model.Budget

data class BudgetWithProgress(
    val budget: Budget,
    val categoryName: String?,
    val spentAmount: Long,
    val remainingAmount: Long,
    val progressPercent: Float,
    val isOverBudget: Boolean,
    val isThresholdReached: Boolean
)

data class BudgetUiState(
    val activeBudgets: List<BudgetWithProgress> = emptyList(),
    val completedBudgets: List<BudgetWithProgress> = emptyList(),
    val categories: List<com.hitunguang.feature.category.domain.model.Category> = emptyList(),
    val showFormDialog: Boolean = false,
    val budgetToEdit: Budget? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
