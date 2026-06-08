package com.hitunguang.feature.budget.domain.usecase

import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import javax.inject.Inject

class UpdateBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) {
        if (budget.amount <= 0L) {
            throw IllegalArgumentException("Nominal budget harus lebih besar dari 0.")
        }
        if (budget.thresholdPercent !in 1..100) {
            throw IllegalArgumentException("Threshold harus berada di antara 1 dan 100%.")
        }
        if (budget.startDate > budget.endDate) {
            throw IllegalArgumentException("Tanggal mulai tidak boleh melebihi tanggal selesai.")
        }
        budgetRepository.updateBudget(budget)
    }
}
