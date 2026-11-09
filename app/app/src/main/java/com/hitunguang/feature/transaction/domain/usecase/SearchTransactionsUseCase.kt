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
        val ftsQuery = toFtsQuery(query) ?: return flowOf(emptyList())
        return repository.searchTransactions(ftsQuery)
    }

    companion object {
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

        // Transform query to FTS prefix: "makan bakso" -> "makan* bakso*".
        // Only letters/digits are kept so FTS syntax chars (" * - ( ) : ^) can't break MATCH.
        fun toFtsQuery(query: String): String? {
            val tokens = query.split(NON_WORD)
                .filter { it.isNotEmpty() }
                .map { it.lowercase() }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { "$it*" }
        }
    }
}
