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
    val error: String? = null,
    val requestNotificationPermission: Boolean = false
) {
    val summary: BudgetSummary get() = BudgetSummary.of(activeBudgets)
}

data class BudgetSummary(
    val limit: Long,
    val spent: Long,
    val remaining: Long
) {
    val progress: Float get() = if (limit > 0) spent.toFloat() / limit.toFloat() else 0f

    companion object {
        /**
         * A GLOBAL budget already covers every category, so when one is active it alone is the
         * summary; otherwise the CATEGORY budgets are summed. Summing both would double count.
         */
        fun of(activeBudgets: List<BudgetWithProgress>): BudgetSummary {
            val scope = activeBudgets.filter { it.budget.budgetType == "GLOBAL" }.ifEmpty { activeBudgets }
            return BudgetSummary(
                limit = scope.sumOf { it.budget.amount },
                spent = scope.sumOf { it.spentAmount },
                remaining = scope.sumOf { it.remainingAmount }
            )
        }
    }
}
