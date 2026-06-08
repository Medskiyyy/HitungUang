package com.hitunguang.core.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hitunguang.core.notification.worker.ReminderWorker
import com.hitunguang.core.notification.worker.ReviewWorker
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    companion object {
        const val WORK_NAME_REMINDER = "DailyReminderWork"
        const val WORK_NAME_WEEKLY = "WeeklyReviewWork"
        const val WORK_NAME_MONTHLY = "MonthlyReviewWork"
    }

    fun scheduleAll(settings: NotificationSettings) {
        scheduleReminder(settings.dailyReminderEnabled, settings.dailyReminderTime)
        scheduleWeeklyReview(settings.weeklyReviewEnabled)
        scheduleMonthlyReview(settings.monthlyReviewEnabled)
    }

    fun scheduleReminder(enabled: Boolean, timeStr: String?) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME_REMINDER)
            return
        }

        val initialDelay = calculateInitialDelayForDaily(timeStr)
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("reminder")
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME_REMINDER,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun scheduleWeeklyReview(enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME_WEEKLY)
            return
        }

        val initialDelay = calculateInitialDelayForWeekly()
        val workRequest = OneTimeWorkRequestBuilder<ReviewWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("weekly_review")
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME_WEEKLY,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun scheduleMonthlyReview(enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME_MONTHLY)
            return
        }

        val initialDelay = calculateInitialDelayForMonthly()
        val workRequest = OneTimeWorkRequestBuilder<ReviewWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("monthly_review")
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME_MONTHLY,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun calculateInitialDelayForDaily(timeStr: String?): Long {
        val time = timeStr ?: "20:00"
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis - now
    }

    private fun calculateInitialDelayForWeekly(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return calendar.timeInMillis - now
    }

    private fun calculateInitialDelayForMonthly(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis - now
    }
}
