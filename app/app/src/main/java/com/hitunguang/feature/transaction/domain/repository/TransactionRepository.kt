package com.hitunguang.feature.transaction.domain.repository

import com.hitunguang.feature.transaction.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionById(id: String): Flow<Transaction?>
    suspend fun insertTransaction(transaction: Transaction, balanceDifference: Long)
    suspend fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction, oldBalanceDiff: Long, newBalanceDiff: Long)
    suspend fun deleteTransaction(transaction: Transaction, balanceDifference: Long)
    fun getAllTransactionsWithDetails(): Flow<List<com.hitunguang.feature.transaction.domain.model.TransactionWithDetails>>
    fun getTransactionWithDetailsById(id: String): Flow<com.hitunguang.feature.transaction.domain.model.TransactionWithDetails?>
    fun searchTransactions(query: String): Flow<List<com.hitunguang.feature.transaction.domain.model.TransactionWithDetails>>
    suspend fun rebuildSearchIndex()
    suspend fun getTransactionByReceiptId(receiptId: String): Transaction?
}
