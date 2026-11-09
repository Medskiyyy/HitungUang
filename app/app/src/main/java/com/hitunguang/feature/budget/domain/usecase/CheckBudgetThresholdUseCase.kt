package com.hitunguang.feature.budget.domain.usecase

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hitunguang.core.notification.worker.BudgetWorker
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class CheckBudgetThresholdUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(categoryId: String?, transactionDate: Long = System.currentTimeMillis()) {
        val allBudgets = budgetRepository.getActiveBudgets().firstOrNull() ?: emptyList()

        // Find active budgets whose period contains the transaction and match its category or are global
        val activeBudgets = allBudgets.filter { budget ->
            budget.isActive &&
            transactionDate in budget.startDate..budget.endDate &&
            (budget.categoryId == null || budget.categoryId == categoryId)
        }

        if (activeBudgets.isEmpty()) return

        val workManager = WorkManager.getInstance(context)
        for (budget in activeBudgets) {
            val inputData = Data.Builder()
                .putString("budget_id", budget.id)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<BudgetWorker>()
                .setInputData(inputData)
                .build()

            // One chain per budget so checks for the same budget never race on the stored alert level
            workManager.enqueueUniqueWork(
                "budget_check_${budget.id}",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
        }
    }
}
