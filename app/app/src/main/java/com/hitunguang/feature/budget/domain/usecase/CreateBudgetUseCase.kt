package com.hitunguang.feature.budget.domain.usecase

import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import javax.inject.Inject

class CreateBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) {
        if (budget.amount <= 0L) {
            throw IllegalArgumentException("Nominal anggaran harus lebih besar dari 0.")
        }
        if (budget.thresholdPercent !in 1..100) {
            throw IllegalArgumentException("Pengingat anggaran harus diisi angka 1–100%.")
        }
        if (budget.startDate > budget.endDate) {
            throw IllegalArgumentException("Tanggal mulai tidak boleh melebihi tanggal selesai.")
        }
        if (budget.budgetType == "CATEGORY" && budget.categoryId == null) {
            throw IllegalArgumentException("Kategori harus dipilih untuk anggaran per kategori.")
        }
        if (budget.budgetType == "GLOBAL" && budget.categoryId != null) {
            throw IllegalArgumentException("Kategori harus kosong untuk anggaran semua kategori.")
        }

        budgetRepository.insertBudget(budget)
    }
}
