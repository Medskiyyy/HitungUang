package com.hitunguang.feature.budget.domain.usecase

import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.util.BudgetDates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class AutoResetBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    companion object {
        // Shared across instances: the daily worker and the dashboard may run this concurrently.
        private val mutex = Mutex()
    }

    suspend operator fun invoke(
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ) = mutex.withLock {
        val expiredBudgets = budgetRepository.getActiveBudgets().first().filter { now > it.endDate }
        expiredBudgets.forEach { budget ->
            // Next calendar-aware period, rolled forward until it contains now
            val (nextStartDate, nextEndDate) = BudgetDates.rollForward(budget.startDate, budget.endDate, now, zone)

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
            budgetRepository.rollOverBudget(budget.copy(isActive = false, updatedAt = now), newBudget)
        }
    }
}
