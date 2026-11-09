package com.hitunguang.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hitunguang.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

data class TransferFeeRow(
    @ColumnInfo(name = "transfer_date")
    val transferDate: Long,
    @ColumnInfo(name = "admin_fee")
    val adminFee: Long
)

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE is_deleted = 0 ORDER BY start_date DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE is_active = 1 AND is_deleted = 0")
    fun getActiveBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET is_deleted = 1, deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDeleteBudget(id: String, now: Long)

    @Query("UPDATE budgets SET is_deleted = 0, deleted_at = NULL, updated_at = :now WHERE id = :id")
    suspend fun restoreBudgetState(id: String, now: Long)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun hardDeleteBudget(id: String)

    @Query("SELECT transfer_date, admin_fee FROM transfers WHERE admin_fee > 0")
    fun getTransferFees(): Flow<List<TransferFeeRow>>

    @Query(
        "SELECT COUNT(*) FROM budgets WHERE is_deleted = 0 AND is_active = 1 AND budget_type = :budgetType " +
            "AND ((:categoryId IS NULL AND category_id IS NULL) OR category_id = :categoryId) AND start_date = :startDate"
    )
    suspend fun countActiveBudgetsStartingAt(budgetType: String, categoryId: String?, startDate: Long): Int

    @Transaction
    suspend fun rollOverBudget(completed: BudgetEntity, successor: BudgetEntity) {
        updateBudget(completed)
        if (countActiveBudgetsStartingAt(successor.budgetType, successor.categoryId, successor.startDate) == 0) {
            insertBudget(successor)
        }
    }
}
