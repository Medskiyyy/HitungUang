package com.hitunguang.core.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.R
import com.hitunguang.core.common.util.CurrencyFormatter
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.core.notification.NotificationHelper
import com.hitunguang.core.notification.NotificationScheduler
import com.hitunguang.core.notification.ReviewPeriods
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.util.BudgetDates
import com.hitunguang.feature.budget.domain.util.BudgetSpending
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId

class ReviewWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReviewWorkerEntryPoint {
        fun notificationHelper(): NotificationHelper
        fun settingsDataStore(): SettingsDataStore
        fun transactionRepository(): TransactionRepository
        fun notificationScheduler(): NotificationScheduler
        fun budgetRepository(): BudgetRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ReviewWorkerEntryPoint::class.java
        )
        val notificationHelper = entryPoint.notificationHelper()
        val settingsDataStore = entryPoint.settingsDataStore()
        val transactionRepository = entryPoint.transactionRepository()
        val scheduler = entryPoint.notificationScheduler()
        val budgetRepository = entryPoint.budgetRepository()

        val settings = settingsDataStore.notificationSettings.firstOrNull() ?: return Result.success()
        val isWeekly = tags.contains("weekly_review")

        return try {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            if (isWeekly) {
                if (!settings.weeklyReviewEnabled) {
                    Timber.d("Weekly review is disabled, skipping.")
                    return Result.success()
                }

                // Previous Monday-Sunday week in local time
                val (weekStart, weekEnd) = ReviewPeriods.previousWeek(today)
                val totalExpense = totalExpenseBetween(
                    transactionRepository, budgetRepository,
                    BudgetDates.startOfDay(weekStart, zoneId), BudgetDates.endOfDay(weekEnd, zoneId)
                )

                notificationHelper.showNotification(
                    channelId = NotificationHelper.CHANNEL_REVIEW,
                    notificationId = NotificationHelper.ID_REVIEW,
                    title = applicationContext.getString(R.string.notif_review_weekly_title),
                    message = applicationContext.getString(R.string.notif_review_weekly_message, CurrencyFormatter.format(totalExpense))
                )

                // Reschedule for next week
                scheduler.scheduleWeeklyReview(true)
            } else {
                if (!settings.monthlyReviewEnabled) {
                    Timber.d("Monthly review is disabled, skipping.")
                    return Result.success()
                }

                // Previous calendar month in local time
                val (monthStart, monthEnd) = ReviewPeriods.previousMonth(today)
                val monthName = ReviewPeriods.monthName(monthStart)
                val totalExpense = totalExpenseBetween(
                    transactionRepository, budgetRepository,
                    BudgetDates.startOfDay(monthStart, zoneId), BudgetDates.endOfDay(monthEnd, zoneId)
                )

                notificationHelper.showNotification(
                    channelId = NotificationHelper.CHANNEL_REVIEW,
                    notificationId = NotificationHelper.ID_REVIEW + 1, // Use a distinct ID for monthly so it doesn't overwrite weekly if both are shown
                    title = applicationContext.getString(R.string.notif_review_monthly_title, monthName),
                    message = applicationContext.getString(R.string.notif_review_monthly_message, monthName, monthStart.year, CurrencyFormatter.format(totalExpense))
                )

                // Reschedule for next month
                scheduler.scheduleMonthlyReview(true)
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error showing review notification")
            Result.retry()
        }
    }

    private suspend fun totalExpenseBetween(
        transactionRepository: TransactionRepository,
        budgetRepository: BudgetRepository,
        startMillis: Long,
        endMillis: Long
    ): Long {
        val transactions = transactionRepository.getAllTransactionsWithDetails().firstOrNull() ?: emptyList()
        val transferFees = budgetRepository.getTransferFees().firstOrNull() ?: emptyList()
        return BudgetSpending.totalExpense(transactions, transferFees, startMillis, endMillis)
    }
}
