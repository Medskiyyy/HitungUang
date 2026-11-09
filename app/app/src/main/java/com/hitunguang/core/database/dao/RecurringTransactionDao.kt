package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hitunguang.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY is_active DESC, next_run_at ASC, title ASC")
    fun getAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1 AND next_run_at <= :now ORDER BY next_run_at ASC")
    suspend fun getDue(now: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RecurringTransactionEntity)

    @Update
    suspend fun update(entity: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET next_run_at = :nextRunAt, updated_at = :now WHERE id = :id")
    suspend fun updateNextRunAt(id: String, nextRunAt: Long, now: Long)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
