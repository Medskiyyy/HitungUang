package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hitunguang.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountById(id: String): Flow<AccountEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT initial_balance FROM accounts WHERE id = :id")
    suspend fun getInitialBalance(id: String): Long

    @Query("SELECT current_balance FROM accounts WHERE id = :id")
    suspend fun getAccountBalance(id: String): Long

    @Query("UPDATE accounts SET current_balance = current_balance + :difference, updated_at = :updatedAt WHERE id = :id")
    suspend fun adjustBalance(id: String, difference: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId")
    suspend fun getTransactionCountForAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM transfers WHERE from_account_id = :accountId OR to_account_id = :accountId")
    suspend fun getTransferCountForAccount(accountId: String): Int

    @Query("UPDATE transactions SET account_id = :newAccountId, updated_at = :updatedAt WHERE account_id = :oldAccountId")
    suspend fun migrateTransactions(oldAccountId: String, newAccountId: String, updatedAt: Long)

    @Query("UPDATE transfers SET from_account_id = :newAccountId, updated_at = :updatedAt WHERE from_account_id = :oldAccountId")
    suspend fun migrateTransfersFrom(oldAccountId: String, newAccountId: String, updatedAt: Long)

    @Query("UPDATE transfers SET to_account_id = :newAccountId, updated_at = :updatedAt WHERE to_account_id = :oldAccountId")
    suspend fun migrateTransfersTo(oldAccountId: String, newAccountId: String, updatedAt: Long)

    @Transaction
    suspend fun transferAccountData(oldAccountId: String, newAccountId: String, balanceToTransfer: Long, updatedAt: Long) {
        migrateTransactions(oldAccountId, newAccountId, updatedAt)
        migrateTransfersFrom(oldAccountId, newAccountId, updatedAt)
        migrateTransfersTo(oldAccountId, newAccountId, updatedAt)
        adjustBalance(newAccountId, balanceToTransfer, updatedAt)
    }
}
