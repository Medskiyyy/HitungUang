package com.hitunguang.feature.recurring.domain.usecase

import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecurringTransactionsUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> = repository.getAll()
}
