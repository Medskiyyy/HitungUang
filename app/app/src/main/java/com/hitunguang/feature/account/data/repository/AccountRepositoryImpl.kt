package com.hitunguang.feature.account.data.repository

import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.feature.account.data.mapper.AccountMapper
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { list ->
            list.map { AccountMapper.toDomain(it) }
        }
    }

    override fun getAccountById(id: String): Flow<Account?> {
        return accountDao.getAccountById(id).map { entity ->
            entity?.let { AccountMapper.toDomain(it) }
        }
    }

    override suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(AccountMapper.toEntity(account))
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(AccountMapper.toEntity(account))
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(AccountMapper.toEntity(account))
    }

    override suspend fun getTransactionCountForAccount(accountId: String): Int {
        return accountDao.getTransactionCountForAccount(accountId)
    }

    override suspend fun getTransferCountForAccount(accountId: String): Int {
        return accountDao.getTransferCountForAccount(accountId)
    }

    override suspend fun transferAccountData(oldAccountId: String, newAccountId: String, balanceToTransfer: Long) {
        accountDao.transferAccountData(oldAccountId, newAccountId, balanceToTransfer, System.currentTimeMillis())
    }
}
