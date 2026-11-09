package com.hitunguang.feature.recurring.domain.usecase

import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.repository.RecurringTransactionRepository
import javax.inject.Inject

class DeleteRecurringTransactionUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(recurring: RecurringTransaction) {
        // Hard delete: transactions already posted by this rule stay
        repository.delete(recurring.id)
    }
}
