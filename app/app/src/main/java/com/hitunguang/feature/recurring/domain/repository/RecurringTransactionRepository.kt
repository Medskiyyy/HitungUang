package com.hitunguang.feature.recurring.domain.repository

import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun getAll(): Flow<List<RecurringTransaction>>
    suspend fun getById(id: String): RecurringTransaction?
    suspend fun getDue(now: Long): List<RecurringTransaction>
    suspend fun insert(recurring: RecurringTransaction)
    suspend fun update(recurring: RecurringTransaction)
    suspend fun updateNextRunAt(id: String, nextRunAt: Long, now: Long)
    suspend fun delete(id: String)

    /** Runs [block] in one database transaction, so a posted occurrence and its next_run_at commit together. */
    suspend fun <R> runInTransaction(block: suspend () -> R): R
}
