package com.hitunguang.core.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.R
import com.hitunguang.core.common.util.CurrencyFormatter
import com.hitunguang.core.notification.BudgetAlertLevel
import com.hitunguang.core.notification.BudgetAlertStore
import com.hitunguang.core.notification.NotificationHelper
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.util.BudgetSpending
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

            val transactions = transactionRepository.getAllTransactionsWithDetails().firstOrNull() ?: emptyList()
            val transferFees = budgetRepository.getTransferFees().firstOrNull() ?: emptyList()
            val spentAmount = BudgetSpending.spentAmount(budget, transactions, transferFees)

            // Notify only when the alert level goes up since the last check of this budget
            val alertStore = BudgetAlertStore(applicationContext)
            val previousLevel = alertStore.lastLevel(budget)
            val currentLevel = BudgetAlertLevel.of(spentAmount, budget.amount, budget.thresholdPercent)
            alertStore.saveLevel(budget, currentLevel)
            if (!BudgetAlertLevel.shouldNotify(previousLevel, currentLevel)) {
                return Result.success()
            }

            val categoryName = if (budget.categoryId != null) {
                val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                categories.find { it.id == budget.categoryId }?.name ?: applicationContext.getString(R.string.notif_budget_category_fallback)
            } else {
                applicationContext.getString(R.string.notif_budget_all_categories)
            }

            val budgetName = if (budget.categoryId != null) applicationContext.getString(R.string.notif_budget_name_category, categoryName) else applicationContext.getString(R.string.notif_budget_all_categories)
            val notificationId = budget.id.hashCode()

            val spentText = CurrencyFormatter.format(spentAmount)
            val limitText = CurrencyFormatter.format(budget.amount)

            if (currentLevel == BudgetAlertLevel.EXCEEDED) {
                notificationHelper.showNotification(
                    channelId = NotificationHelper.CHANNEL_BUDGET,
                    notificationId = notificationId,
                    title = applicationContext.getString(R.string.notif_budget_exceeded_title),
                    message = applicationContext.getString(R.string.notif_budget_exceeded_message, budgetName, spentText, limitText)
                )
            } else {
                notificationHelper.showNotification(
                    channelId = NotificationHelper.CHANNEL_BUDGET,
                    notificationId = notificationId,
                    title = applicationContext.getString(R.string.notif_budget_near_title),
                    message = applicationContext.getString(R.string.notif_budget_near_message, budgetName, budget.thresholdPercent, spentText, limitText)
                )
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error executing BudgetWorker")
            Result.retry()
        }
    }
}
