package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hitunguang.core.database.entity.ReceiptEntity
import com.hitunguang.core.database.entity.ReceiptItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY receipt_date DESC, created_at DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun getReceiptById(id: String): Flow<ReceiptEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipt_items WHERE receipt_id = :receiptId ORDER BY created_at ASC")
    fun getReceiptItems(receiptId: String): Flow<List<ReceiptItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptItems(items: List<ReceiptItemEntity>)

    @Query("DELETE FROM receipt_items WHERE receipt_id = :receiptId")
    suspend fun deleteItemsForReceipt(receiptId: String)

    @Transaction
    suspend fun insertReceiptWithItems(receipt: ReceiptEntity, items: List<ReceiptItemEntity>) {
        insertReceipt(receipt)
        deleteItemsForReceipt(receipt.id)
        insertReceiptItems(items)
    }
}
