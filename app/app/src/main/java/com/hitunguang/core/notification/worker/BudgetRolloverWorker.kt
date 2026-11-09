package com.hitunguang.core.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hitunguang.feature.budget.domain.usecase.AutoResetBudgetsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

class BudgetRolloverWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BudgetRolloverWorkerEntryPoint {
        fun autoResetBudgetsUseCase(): AutoResetBudgetsUseCase
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                BudgetRolloverWorkerEntryPoint::class.java
            )
            entryPoint.autoResetBudgetsUseCase()()
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error executing BudgetRolloverWorker")
            Result.retry()
        }
    }
}
