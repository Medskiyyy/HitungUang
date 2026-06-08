package com.hitunguang.core.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.core.notification.NotificationHelper
import com.hitunguang.core.notification.NotificationScheduler
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

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

        val settings = settingsDataStore.notificationSettings.firstOrNull() ?: return Result.success()
        val isWeekly = tags.contains("weekly_review")

        return try {
            val now = System.currentTimeMillis()
            if (isWeekly) {
                if (!settings.weeklyReviewEnabled) {
                    Timber.d("Weekly review is disabled, skipping.")
                    return Result.success()
                }

                // Calculate last 7 days total expenses
                val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000L
                val transactions = transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()
                val totalExpense = transactions
                    .filter { it.transactionType == "EXPENSE" && !it.isDeleted && it.transactionDate in sevenDaysAgo..now }
                    .sumOf { it.amount }

                notificationHelper.showNotification(
                    channelId = NotificationHelper.CHANNEL_REVIEW,
                    notificationId = NotificationHelper.ID_REVIEW,
                    title = "Review Keuangan Mingguan",
                    message = "Total pengeluaran Anda minggu ini adalah Rp $totalExpense."
                )

                // Reschedule for next week
                scheduler.scheduleWeeklyReview(true)
            } else {
                if (!settings.monthlyReviewEnabled) {
                    Timber.d("Monthly review is disabled, skipping.")
                    return Result.success()
                }

                // Calculate last 30 days total expenses
                val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L
                val transactions = transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()
                val totalExpense = transactions
                    .filter { it.transactionType == "EXPENSE" && !it.isDeleted && it.transactionDate in thirtyDaysAgo..now }
                    .sumOf { it.amount }

                notificationHelper.showNotification(
                    channelId = NotificationHelper.CHANNEL_REVIEW,
                    notificationId = NotificationHelper.ID_REVIEW + 1, // Use a distinct ID for monthly so it doesn't overwrite weekly if both are shown
                    title = "Review Keuangan Bulanan",
                    message = "Total pengeluaran Anda bulan ini adalah Rp $totalExpense."
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
}
