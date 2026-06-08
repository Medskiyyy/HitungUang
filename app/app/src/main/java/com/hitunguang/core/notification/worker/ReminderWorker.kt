package com.hitunguang.core.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.core.notification.NotificationHelper
import com.hitunguang.core.notification.NotificationScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderWorkerEntryPoint {
        fun notificationHelper(): NotificationHelper
        fun settingsDataStore(): SettingsDataStore
        fun notificationScheduler(): NotificationScheduler
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderWorkerEntryPoint::class.java
        )
        val notificationHelper = entryPoint.notificationHelper()
        val settingsDataStore = entryPoint.settingsDataStore()
        val scheduler = entryPoint.notificationScheduler()

        return try {
            val settings = settingsDataStore.notificationSettings.firstOrNull()
            if (settings == null || !settings.dailyReminderEnabled) {
                Timber.d("Daily reminder is disabled, skipping.")
                return Result.success()
            }

            notificationHelper.showNotification(
                channelId = NotificationHelper.CHANNEL_REMINDER,
                notificationId = NotificationHelper.ID_REMINDER,
                title = "Catat Keuangan Anda",
                message = "Jangan lupa untuk mencatat pengeluaran dan pemasukan Anda hari ini!"
            )

            // Reschedule daily reminder for tomorrow
            scheduler.scheduleReminder(true, settings.dailyReminderTime)

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error showing daily reminder")
            Result.retry()
        }
    }
}
