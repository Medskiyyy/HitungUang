package com.hitunguang

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hitunguang.core.backup.worker.DailyBackupWorker
import com.hitunguang.core.backup.worker.WeeklyBackupWorker
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.recyclebin.data.worker.AutoCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HitungUangApp : Application() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate() {
        super.onCreate()

        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }

        scheduleRecycleBinCleanup()

        // Schedule backup workers based on persisted settings
        CoroutineScope(Dispatchers.IO).launch {
            scheduleBackupWorkers()
        }
    }

    private fun scheduleRecycleBinCleanup() {
        val workRequest = PeriodicWorkRequestBuilder<AutoCleanupWorker>(
            24, TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecycleBinCleanupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private suspend fun scheduleBackupWorkers() {
        val settings = settingsDataStore.backupSettings.firstOrNull() ?: return
        if (!settings.autoBackupEnabled || settings.backupFolderUri == null) return

        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()
        val workManager = WorkManager.getInstance(this)

        when (settings.backupFrequency) {
            "DAILY", "REALTIME" -> {
                val dailyRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints).build()
                workManager.enqueueUniquePeriodicWork(
                    "DailyBackupWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    dailyRequest
                )
            }
            "WEEKLY" -> {
                val weeklyRequest = PeriodicWorkRequestBuilder<WeeklyBackupWorker>(7, TimeUnit.DAYS)
                    .setConstraints(constraints).build()
                workManager.enqueueUniquePeriodicWork(
                    "WeeklyBackupWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    weeklyRequest
                )
            }
        }
        Timber.d("Backup workers dijadwalkan: ${settings.backupFrequency}")
    }
}

