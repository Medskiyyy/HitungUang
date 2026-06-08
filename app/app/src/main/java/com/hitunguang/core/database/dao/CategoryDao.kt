package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.RecycleBinEntity
import com.hitunguang.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY is_pinned DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE category_type = :type ORDER BY is_pinned DESC, name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: String): Flow<CategoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId AND is_deleted = 0")
    suspend fun getActiveTransactionsForCategory(categoryId: String): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId AND is_deleted = 0")
    suspend fun getTransactionCountForCategory(categoryId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecycleBinEntry(entry: RecycleBinEntity)

    @Query("UPDATE accounts SET current_balance = current_balance + :difference, updated_at = :updatedAt WHERE id = :accountId")
    suspend fun adjustAccountBalance(accountId: String, difference: Long, updatedAt: Long)

    @Query("UPDATE transactions SET is_deleted = 1, deleted_at = CASE WHEN is_deleted = 0 THEN :now ELSE deleted_at END, category_id = NULL, updated_at = :now WHERE category_id = :categoryId")
    suspend fun updateTransactionsForDeletedCategory(categoryId: String, now: Long)

    @Transaction
    suspend fun deleteCategoryAndSoftDeleteTransactions(category: CategoryEntity, now: Long) {
        val activeTransactions = getActiveTransactionsForCategory(category.id)
        for (tx in activeTransactions) {
            val diff = when (tx.transactionType) {
                "INCOME" -> -tx.amount
                "EXPENSE", "TRANSFER_FEE" -> tx.amount
                else -> 0L
            }
            if (diff != 0L) {
                adjustAccountBalance(tx.accountId, diff, now)
            }
            val recycleBinId = UUID.randomUUID().toString()
            val expireAt = now + 30L * 24 * 60 * 60 * 1000L
            val recycleBinEntry = RecycleBinEntity(
                id = recycleBinId,
                entityType = "TRANSACTION",
                entityId = tx.id,
                deletedAt = now,
                expireAt = expireAt
            )
            insertRecycleBinEntry(recycleBinEntry)
        }
        updateTransactionsForDeletedCategory(category.id, now)
        deleteCategory(category)
    }
}
