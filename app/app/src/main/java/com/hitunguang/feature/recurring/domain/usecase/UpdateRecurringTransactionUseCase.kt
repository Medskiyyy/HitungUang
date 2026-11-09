package com.hitunguang.feature.recurring.domain.usecase

import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.repository.RecurringTransactionRepository
import javax.inject.Inject

class UpdateRecurringTransactionUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(recurring: RecurringTransaction) {
        validateRecurring(recurring)
        repository.update(recurring)
    }
}
