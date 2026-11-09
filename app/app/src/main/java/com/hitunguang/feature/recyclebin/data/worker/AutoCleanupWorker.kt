package com.hitunguang.feature.recyclebin.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.feature.recyclebin.domain.usecase.CleanupExpiredItemsUseCase
import com.hitunguang.feature.receipt.domain.usecase.AutoDeleteReceiptsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

class AutoCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutoCleanupWorkerEntryPoint {
        fun cleanupExpiredItemsUseCase(): CleanupExpiredItemsUseCase
        fun autoDeleteReceiptsUseCase(): AutoDeleteReceiptsUseCase
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AutoCleanupWorkerEntryPoint::class.java
        )
        val cleanupExpiredItemsUseCase = entryPoint.cleanupExpiredItemsUseCase()
        val autoDeleteReceiptsUseCase = entryPoint.autoDeleteReceiptsUseCase()

        Timber.d("Starting Recycle Bin auto-cleanup...")
        val binFailedCompletely = try {
            val result = cleanupExpiredItemsUseCase(System.currentTimeMillis())
            Timber.d("Recycle Bin auto-cleanup finished: ${result.deleted} deleted, ${result.failed} failed.")
            result.failed > 0 && result.deleted == 0
        } catch (e: Exception) {
            Timber.e(e, "Error occurred during Recycle Bin auto-cleanup")
            true
        }

        val receiptsFailed = try {
            Timber.d("Starting expired receipts auto-cleanup...")
            autoDeleteReceiptsUseCase()
            Timber.d("Expired receipts auto-cleanup finished successfully.")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error occurred during receipts auto-cleanup")
            true
        }

        return if (binFailedCompletely || receiptsFailed) Result.retry() else Result.success()
    }
}
