package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hitunguang.core.database.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY transfer_date DESC, created_at DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    fun getTransferById(id: String): Flow<TransferEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransfer(transfer: TransferEntity)

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transfers WHERE to_account_id = :accountId")
    suspend fun sumTransfersIn(accountId: String): Long

    @Query("SELECT COALESCE(SUM(amount + admin_fee), 0) FROM transfers WHERE from_account_id = :accountId")
    suspend fun sumTransfersOut(accountId: String): Long

    @Query("UPDATE accounts SET current_balance = current_balance + :difference, updated_at = :updatedAt WHERE id = :id")
    suspend fun adjustAccountBalance(id: String, difference: Long, updatedAt: Long)

    @Transaction
    suspend fun executeTransfer(transfer: TransferEntity) {
        insertTransfer(transfer)
        // Deduct from sender account (amount + admin fee)
        adjustAccountBalance(transfer.fromAccountId, -(transfer.amount + transfer.adminFee), transfer.createdAt)
        // Add to receiver account
        adjustAccountBalance(transfer.toAccountId, transfer.amount, transfer.createdAt)
    }

    @Transaction
    suspend fun revertTransfer(transfer: TransferEntity) {
        deleteTransfer(transfer)
        // Refund sender account (amount + admin fee)
        adjustAccountBalance(transfer.fromAccountId, transfer.amount + transfer.adminFee, System.currentTimeMillis())
        // Deduct from receiver account
        adjustAccountBalance(transfer.toAccountId, -transfer.amount, System.currentTimeMillis())
    }
}
