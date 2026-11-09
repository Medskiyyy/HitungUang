package com.hitunguang.feature.budget.domain.repository

import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.model.TransferFee
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<Budget>>
    fun getActiveBudgets(): Flow<List<Budget>>
    suspend fun insertBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)

    fun getTransferFees(): Flow<List<TransferFee>> = flowOf(emptyList())

    /** Marks [completed] inactive and inserts [successor] unless an active successor already exists. */
    suspend fun rollOverBudget(completed: Budget, successor: Budget) {
        updateBudget(completed)
        insertBudget(successor)
    }
}
