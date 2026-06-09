package com.hitunguang.feature.account.data.repository

import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.feature.account.data.mapper.AccountMapper
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import com.hitunguang.core.database.dao.RecycleBinDao
import com.hitunguang.core.database.entity.RecycleBinEntity
import java.util.UUID

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val recycleBinDao: RecycleBinDao? = null
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
        val now = System.currentTimeMillis()
        accountDao.softDeleteAccount(account.id, now)
        val recycleBinId = UUID.randomUUID().toString()
        val expireAt = now + 30L * 24 * 60 * 60 * 1000L
        val recycleBinEntry = RecycleBinEntity(
            id = recycleBinId,
            entityType = "WALLET",
            entityId = account.id,
            deletedAt = now,
            expireAt = expireAt
        )
        recycleBinDao?.addToRecycleBin(recycleBinEntry)
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
