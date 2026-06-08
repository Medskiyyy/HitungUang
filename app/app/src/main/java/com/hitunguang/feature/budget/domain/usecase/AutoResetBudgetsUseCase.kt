package com.hitunguang.feature.budget.domain.usecase

import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import java.util.UUID
import javax.inject.Inject

class AutoResetBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budgets: List<Budget>) {
        val now = System.currentTimeMillis()
        budgets.forEach { budget ->
            if (budget.isActive && now > budget.endDate) {
                // 1. Mark current budget as inactive (completed)
                val completedBudget = budget.copy(isActive = false, updatedAt = now)
                budgetRepository.updateBudget(completedBudget)

                // 2. Create a new budget for the next period, shifting until it covers the current time
                val duration = budget.endDate - budget.startDate
                val step = duration + 1L
                val steps = Math.ceil((now - budget.endDate).toDouble() / step.toDouble()).toLong()
                val nextStartDate = budget.startDate + steps * step
                val nextEndDate = budget.endDate + steps * step

                val newBudget = Budget(
                    id = UUID.randomUUID().toString(),
                    categoryId = budget.categoryId,
                    budgetType = budget.budgetType,
                    amount = budget.amount,
                    thresholdPercent = budget.thresholdPercent,
                    startDate = nextStartDate,
                    endDate = nextEndDate,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
                budgetRepository.insertBudget(newBudget)
            }
        }
    }
}
