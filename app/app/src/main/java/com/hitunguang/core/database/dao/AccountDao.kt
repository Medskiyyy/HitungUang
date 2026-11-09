package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.RecycleBinEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE is_deleted = 0 ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id AND is_deleted = 0")
    fun getAccountById(id: String): Flow<AccountEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET name = :name, account_type = :accountType, icon = :icon, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateAccountDetails(id: String, name: String, accountType: String, icon: String?, updatedAt: Long)

    @Query("UPDATE accounts SET is_deleted = 1, deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDeleteAccount(id: String, now: Long)

    @Query("UPDATE accounts SET is_deleted = 0, deleted_at = NULL, updated_at = :now WHERE id = :id")
    suspend fun restoreAccountState(id: String, now: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun hardDeleteAccount(id: String)

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

    @Query("UPDATE recurring_transactions SET account_id = :newAccountId, updated_at = :updatedAt WHERE account_id = :oldAccountId")
    suspend fun migrateRecurringTransactions(oldAccountId: String, newAccountId: String, updatedAt: Long)

    @Query("UPDATE recurring_transactions SET is_active = 0, updated_at = :updatedAt WHERE account_id = :accountId")
    suspend fun deactivateRecurringTransactions(accountId: String, updatedAt: Long)

    @Query("DELETE FROM recurring_transactions WHERE account_id = :accountId")
    suspend fun deleteRecurringTransactionsForAccount(accountId: String)

    @Transaction
    suspend fun hardDeleteAccountWithRecurring(id: String) {
        // recurring_transactions.account_id is RESTRICT: its rules have to go first
        deleteRecurringTransactionsForAccount(id)
        hardDeleteAccount(id)
    }

    @Query("UPDATE accounts SET current_balance = 0, initial_balance = 0, updated_at = :updatedAt WHERE id = :id")
    suspend fun clearBalances(id: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecycleBinEntry(entry: RecycleBinEntity)

    @Transaction
    suspend fun transferAccountData(oldAccountId: String, newAccountId: String, balanceToTransfer: Long, updatedAt: Long) {
        migrateTransactions(oldAccountId, newAccountId, updatedAt)
        migrateTransfersFrom(oldAccountId, newAccountId, updatedAt)
        migrateTransfersTo(oldAccountId, newAccountId, updatedAt)
        adjustBalance(newAccountId, balanceToTransfer, updatedAt)
        clearBalances(oldAccountId, updatedAt)
    }

    @Transaction
    suspend fun deleteAccountAndMoveData(accountId: String, replacementAccountId: String?, now: Long) {
        if (replacementAccountId != null) {
            transferAccountData(accountId, replacementAccountId, getAccountBalance(accountId), now)
            migrateRecurringTransactions(accountId, replacementAccountId, now)
        } else {
            // No wallet to post into anymore; restoring the wallet lets the user resume them
            deactivateRecurringTransactions(accountId, now)
        }
        softDeleteAccount(accountId, now)
        insertRecycleBinEntry(
            RecycleBinEntity(
                id = UUID.randomUUID().toString(),
                entityType = "WALLET",
                entityId = accountId,
                deletedAt = now,
                expireAt = now + 30L * 24 * 60 * 60 * 1000L
            )
        )
    }
}
