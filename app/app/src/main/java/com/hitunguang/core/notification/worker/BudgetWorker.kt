package com.hitunguang.core.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.core.notification.NotificationHelper
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

class BudgetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BudgetWorkerEntryPoint {
        fun notificationHelper(): NotificationHelper
        fun budgetRepository(): BudgetRepository
        fun transactionRepository(): TransactionRepository
        fun categoryRepository(): CategoryRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BudgetWorkerEntryPoint::class.java
        )
        val notificationHelper = entryPoint.notificationHelper()
        val budgetRepository = entryPoint.budgetRepository()
        val transactionRepository = entryPoint.transactionRepository()
        val categoryRepository = entryPoint.categoryRepository()

        val budgetId = inputData.getString("budget_id") ?: return Result.failure()

        return try {
            val budgets = budgetRepository.getAllBudgets().firstOrNull() ?: emptyList()
            val budget = budgets.find { it.id == budgetId } ?: return Result.success()

            if (!budget.isActive) {
                Timber.d("Budget is not active, skipping.")
                return Result.success()
            }

            val transactions = transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()

            // Filter transactions inside budget range
            val spentAmount = transactions
                .filter { tx ->
                    tx.transactionType == "EXPENSE" &&
                    !tx.isDeleted &&
                    tx.transactionDate in budget.startDate..budget.endDate &&
                    (budget.categoryId == null || tx.categoryId == budget.categoryId)
                }
                .sumOf { it.amount }

            val thresholdAmount = budget.amount * (budget.thresholdPercent / 100.0)

            val categoryName = if (budget.categoryId != null) {
                val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                categories.find { it.id == budget.categoryId }?.name ?: "Kategori"
            } else {
                "Global"
            }

            val budgetName = if (budget.categoryId != null) "Kategori $categoryName" else "Global"
            val notificationId = budget.id.hashCode()

            if (spentAmount >= budget.amount) {
                notificationHelper.showNotification(
                    channelId = NotificationHelper.CHANNEL_BUDGET,
                    notificationId = notificationId,
                    title = "Anggaran Habis!",
                    message = "Anggaran $budgetName Anda telah habis (Rp $spentAmount dari Rp ${budget.amount})."
                )
            } else if (spentAmount >= thresholdAmount) {
                notificationHelper.showNotification(
                    channelId = NotificationHelper.CHANNEL_BUDGET,
                    notificationId = notificationId,
                    title = "Anggaran Hampir Habis!",
                    message = "Anggaran $budgetName Anda telah melewati batas threshold ${budget.thresholdPercent}% (Rp $spentAmount dari Rp ${budget.amount})."
                )
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error executing BudgetWorker")
            Result.retry()
        }
    }
}
