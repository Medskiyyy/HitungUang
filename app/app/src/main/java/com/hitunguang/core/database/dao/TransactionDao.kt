package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.core.database.entity.TransactionWithDetailsEntity
import com.hitunguang.core.database.entity.RecycleBinEntity
import com.hitunguang.core.database.entity.TransactionSearchEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY transaction_date DESC, created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: String): Flow<TransactionEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("""
        SELECT t.*, a.name AS account_name, c.name AS category_name, c.icon AS category_icon 
        FROM transactions t 
        JOIN accounts a ON t.account_id = a.id 
        LEFT JOIN categories c ON t.category_id = c.id 
        WHERE t.is_deleted = 0 
        ORDER BY t.transaction_date DESC, t.created_at DESC
    """)
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetailsEntity>>

    @Query("""
        SELECT t.*, a.name AS account_name, c.name AS category_name, c.icon AS category_icon 
        FROM transactions t 
        JOIN accounts a ON t.account_id = a.id 
        LEFT JOIN categories c ON t.category_id = c.id 
        WHERE t.id = :id
    """)
    fun getTransactionWithDetailsById(id: String): Flow<TransactionWithDetailsEntity?>

    @Query("UPDATE transactions SET is_deleted = 1, deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, now: Long)

    @Query("UPDATE transactions SET is_deleted = 0, deleted_at = NULL, updated_at = :now WHERE id = :id")
    suspend fun restoreTransactionState(id: String, now: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionEntityById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE receipt_id = :receiptId LIMIT 1")
    suspend fun getTransactionByReceiptId(receiptId: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDeleteTransaction(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecycleBinEntry(entry: RecycleBinEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account_id = :accountId AND transaction_type = :type AND is_deleted = 0")
    suspend fun sumByTypeAndAccount(accountId: String, type: String): Long

    @Query("UPDATE accounts SET current_balance = current_balance + :difference, updated_at = :updatedAt WHERE id = :id")
    suspend fun adjustAccountBalance(id: String, difference: Long, updatedAt: Long)

    @Transaction
    suspend fun insertTransactionAndUpdateBalance(transaction: TransactionEntity, balanceDifference: Long) {
        insertTransaction(transaction)
        adjustAccountBalance(transaction.accountId, balanceDifference, transaction.updatedAt)
        indexTransaction(transaction.id, transaction.title, transaction.note, transaction.categoryId, transaction.receiptId)
    }

    @Transaction
    suspend fun deleteTransactionAndUpdateBalance(transaction: TransactionEntity, balanceDifference: Long) {
        val now = System.currentTimeMillis()
        softDeleteTransaction(transaction.id, now)
        adjustAccountBalance(transaction.accountId, balanceDifference, now)
        deindexTransaction(transaction.id)
        
        val recycleBinId = UUID.randomUUID().toString()
        val expireAt = now + 30L * 24 * 60 * 60 * 1000L
        val recycleBinEntry = RecycleBinEntity(
            id = recycleBinId,
            entityType = "TRANSACTION",
            entityId = transaction.id,
            deletedAt = now,
            expireAt = expireAt
        )
        insertRecycleBinEntry(recycleBinEntry)
    }
    
    @Transaction
    suspend fun updateTransactionAndUpdateBalance(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity,
        oldBalanceDiff: Long,
        newBalanceDiff: Long
    ) {
        // If account changed
        if (oldTransaction.accountId != newTransaction.accountId) {
            // Revert old transaction balance from old account
            adjustAccountBalance(oldTransaction.accountId, -oldBalanceDiff, newTransaction.updatedAt)
            // Apply new transaction balance to new account
            adjustAccountBalance(newTransaction.accountId, newBalanceDiff, newTransaction.updatedAt)
        } else {
            // Adjust balance on the same account
            val netDifference = newBalanceDiff - oldBalanceDiff
            adjustAccountBalance(newTransaction.accountId, netDifference, newTransaction.updatedAt)
        }
        updateTransaction(newTransaction)
        indexTransaction(newTransaction.id, newTransaction.title, newTransaction.note, newTransaction.categoryId, newTransaction.receiptId)
    }

    @Query("INSERT INTO transaction_search (transaction_id, transaction_title, transaction_note, category_name, receipt_item_name) VALUES (:id, :title, :note, :categoryName, :receiptItemName)")
    suspend fun insertSearchIndex(id: String, title: String, note: String?, categoryName: String?, receiptItemName: String?)

    @Query("SELECT name FROM categories WHERE id = :categoryId")
    suspend fun getCategoryNameById(categoryId: String): String?

    @Query("SELECT group_concat(item_name, ' ') FROM receipt_items WHERE receipt_id = :receiptId")
    suspend fun getReceiptItemNamesByReceiptId(receiptId: String): String?

    @Transaction
    suspend fun indexTransaction(id: String, title: String, note: String?, categoryId: String?, receiptId: String?) {
        deindexTransaction(id)
        val categoryName = categoryId?.let { getCategoryNameById(it) }
        val receiptItemName = receiptId?.let { getReceiptItemNamesByReceiptId(it) }
        insertSearchIndex(id, title, note, categoryName, receiptItemName)
    }

    @Query("DELETE FROM transaction_search WHERE transaction_id = :id")
    suspend fun deindexTransaction(id: String)

    @Query("DELETE FROM transaction_search")
    suspend fun clearSearchIndex()

    @Query("""
        INSERT INTO transaction_search (transaction_id, transaction_title, transaction_note, category_name, receipt_item_name)
        SELECT t.id, t.title, t.note, c.name, 
               (SELECT group_concat(ri.item_name, ' ') FROM receipt_items ri WHERE ri.receipt_id = t.receipt_id)
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.id
        WHERE t.is_deleted = 0
    """)
    suspend fun rebuildSearchIndexInternal()

    @Transaction
    suspend fun rebuildSearchIndex() {
        clearSearchIndex()
        rebuildSearchIndexInternal()
    }

    @Query("""
        SELECT t.*, a.name AS account_name, c.name AS category_name, c.icon AS category_icon 
        FROM transactions t 
        JOIN accounts a ON t.account_id = a.id 
        LEFT JOIN categories c ON t.category_id = c.id 
        JOIN transaction_search s ON t.id = s.transaction_id
        WHERE t.is_deleted = 0 AND transaction_search MATCH :query
        ORDER BY t.transaction_date DESC, t.created_at DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionWithDetailsEntity>>
}
