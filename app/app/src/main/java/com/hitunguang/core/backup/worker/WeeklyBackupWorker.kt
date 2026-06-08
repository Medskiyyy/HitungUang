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

class WeeklyBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WeeklyBackupWorkerEntryPoint {
        fun createBackupUseCase(): CreateBackupUseCase
        fun getBackupSettingsUseCase(): GetBackupSettingsUseCase
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WeeklyBackupWorkerEntryPoint::class.java
        )
        val getBackupSettings = entryPoint.getBackupSettingsUseCase()
        val createBackup = entryPoint.createBackupUseCase()

        return try {
            val settings = getBackupSettings() ?: return Result.success()
            if (!settings.autoBackupEnabled) return Result.success()
            if (settings.backupFrequency != "WEEKLY") return Result.success()
            val folderUri = settings.backupFolderUri ?: return Result.success()

            Timber.d("Weekly backup dimulai...")
            createBackup(folderUri).fold(
                onSuccess = {
                    Timber.i("Weekly backup berhasil: $it")
                    Result.success()
                },
                onFailure = {
                    Timber.e(it, "Weekly backup gagal")
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error saat weekly backup")
            Result.retry()
        }
    }
}
