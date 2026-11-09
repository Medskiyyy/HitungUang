package com.hitunguang.core.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.feature.recurring.domain.usecase.ProcessDueRecurringUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

class RecurringTransactionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecurringTransactionWorkerEntryPoint {
        fun processDueRecurringUseCase(): ProcessDueRecurringUseCase
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                RecurringTransactionWorkerEntryPoint::class.java
            )
            entryPoint.processDueRecurringUseCase()()
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error executing RecurringTransactionWorker")
            Result.retry()
        }
    }
}
