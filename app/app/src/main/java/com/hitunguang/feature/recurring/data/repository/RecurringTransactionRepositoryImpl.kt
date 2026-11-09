package com.hitunguang.feature.recurring.data.repository

import androidx.room.withTransaction
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.dao.RecurringTransactionDao
import com.hitunguang.feature.recurring.data.mapper.RecurringTransactionMapper
import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecurringTransactionRepositoryImpl @Inject constructor(
    private val database: HitungUangDatabase,
    private val recurringTransactionDao: RecurringTransactionDao
) : RecurringTransactionRepository {

    override fun getAll(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAll().map { list ->
            list.map { RecurringTransactionMapper.toDomain(it) }
        }
    }

    override suspend fun getById(id: String): RecurringTransaction? {
        return recurringTransactionDao.getById(id)?.let { RecurringTransactionMapper.toDomain(it) }
    }

    override suspend fun getDue(now: Long): List<RecurringTransaction> {
        return recurringTransactionDao.getDue(now).map { RecurringTransactionMapper.toDomain(it) }
    }

    override suspend fun insert(recurring: RecurringTransaction) {
        recurringTransactionDao.insert(RecurringTransactionMapper.toEntity(recurring))
    }

    override suspend fun update(recurring: RecurringTransaction) {
        recurringTransactionDao.update(RecurringTransactionMapper.toEntity(recurring))
    }

    override suspend fun updateNextRunAt(id: String, nextRunAt: Long, now: Long) {
        recurringTransactionDao.updateNextRunAt(id, nextRunAt, now)
    }

    override suspend fun delete(id: String) {
        recurringTransactionDao.deleteById(id)
    }

    override suspend fun <R> runInTransaction(block: suspend () -> R): R {
        return database.withTransaction { block() }
    }
}
