package com.hitunguang.feature.account.domain.repository

import com.hitunguang.feature.account.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getAccountById(id: String): Flow<Account?>
    suspend fun insertAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    suspend fun getTransactionCountForAccount(accountId: String): Int
    suspend fun getTransferCountForAccount(accountId: String): Int
    suspend fun transferAccountData(oldAccountId: String, newAccountId: String, balanceToTransfer: Long)
}
