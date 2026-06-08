package com.hitunguang.core.backup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.feature.backup.domain.usecase.CreateBackupUseCase
import com.hitunguang.feature.backup.domain.usecase.GetBackupSettingsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

class DailyBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailyBackupWorkerEntryPoint {
        fun createBackupUseCase(): CreateBackupUseCase
        fun getBackupSettingsUseCase(): GetBackupSettingsUseCase
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DailyBackupWorkerEntryPoint::class.java
        )
        val getBackupSettings = entryPoint.getBackupSettingsUseCase()
        val createBackup = entryPoint.createBackupUseCase()

        return try {
            val settings = getBackupSettings() ?: return Result.success()
            if (!settings.autoBackupEnabled) return Result.success()
            if (settings.backupFrequency !in listOf("DAILY", "REALTIME")) return Result.success()
            val folderUri = settings.backupFolderUri ?: return Result.success()

            Timber.d("Daily backup dimulai...")
            createBackup(folderUri).fold(
                onSuccess = {
                    Timber.i("Daily backup berhasil: $it")
                    Result.success()
                },
                onFailure = {
                    Timber.e(it, "Daily backup gagal")
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error saat daily backup")
            Result.retry()
        }
    }
}
