package com.hitunguang.feature.transaction.data.repository

import com.hitunguang.core.database.dao.TransactionDao
import com.hitunguang.feature.transaction.data.mapper.TransactionMapper
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list ->
            list.map { TransactionMapper.toDomain(it) }
        }
    }

    override fun getTransactionById(id: String): Flow<Transaction?> {
        return transactionDao.getTransactionById(id).map { entity ->
            entity?.let { TransactionMapper.toDomain(it) }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction, balanceDifference: Long) {
        transactionDao.insertTransactionAndUpdateBalance(
            TransactionMapper.toEntity(transaction),
            balanceDifference
        )
    }

    override suspend fun updateTransaction(
        oldTransaction: Transaction,
        newTransaction: Transaction,
        oldBalanceDiff: Long,
        newBalanceDiff: Long
    ) {
        transactionDao.updateTransactionAndUpdateBalance(
            TransactionMapper.toEntity(oldTransaction),
            TransactionMapper.toEntity(newTransaction),
            oldBalanceDiff,
            newBalanceDiff
        )
    }

    override suspend fun deleteTransaction(transaction: Transaction, balanceDifference: Long) {
        transactionDao.deleteTransactionAndUpdateBalance(
            TransactionMapper.toEntity(transaction),
            balanceDifference
        )
    }

    override fun getAllTransactionsWithDetails(): Flow<List<com.hitunguang.feature.transaction.domain.model.TransactionWithDetails>> {
        return transactionDao.getAllTransactionsWithDetails().map { list ->
            list.map { TransactionMapper.toDomainWithDetails(it) }
        }
    }

    override fun getTransactionWithDetailsById(id: String): Flow<com.hitunguang.feature.transaction.domain.model.TransactionWithDetails?> {
        return transactionDao.getTransactionWithDetailsById(id).map { entity ->
            entity?.let { TransactionMapper.toDomainWithDetails(it) }
        }
    }

    override fun searchTransactions(query: String): Flow<List<com.hitunguang.feature.transaction.domain.model.TransactionWithDetails>> {
        return transactionDao.searchTransactions(query).map { list ->
            list.map { TransactionMapper.toDomainWithDetails(it) }
        }
    }

    override suspend fun rebuildSearchIndex() {
        transactionDao.rebuildSearchIndex()
    }

    override suspend fun getTransactionByReceiptId(receiptId: String): Transaction? {
        return transactionDao.getTransactionByReceiptId(receiptId)?.let {
            TransactionMapper.toDomain(it)
        }
    }
}
