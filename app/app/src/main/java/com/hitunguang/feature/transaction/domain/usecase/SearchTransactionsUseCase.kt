package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(query: String): Flow<List<TransactionWithDetails>> {
        val clean = query.trim()
        if (clean.isEmpty()) {
            return flowOf(emptyList())
        }
        
        // Transform query to FTS prefix: "makan bakso" -> "makan* bakso*"
        val ftsQuery = clean.split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .joinToString(" ") { "$it*" }
            
        return repository.searchTransactions(ftsQuery)
    }
}
