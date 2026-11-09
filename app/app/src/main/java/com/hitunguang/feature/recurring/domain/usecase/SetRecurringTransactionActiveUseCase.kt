package com.hitunguang.feature.recurring.domain.usecase

import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.repository.RecurringTransactionRepository
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class SetRecurringTransactionActiveUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(recurring: RecurringTransaction, active: Boolean, now: Long = System.currentTimeMillis()) {
        // Resuming must not back-post what was skipped while paused; today's occurrence still counts
        val nextRunAt = if (active && !recurring.isActive) {
            val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
            RecurrenceSchedule.skipPast(recurring.recurrenceRule, recurring.nextRunAt, today)
        } else {
            recurring.nextRunAt
        }
        repository.update(recurring.copy(isActive = active, nextRunAt = nextRunAt, updatedAt = now))
    }
}
