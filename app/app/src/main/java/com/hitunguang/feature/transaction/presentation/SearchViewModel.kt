package com.hitunguang.feature.transaction.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import com.hitunguang.feature.transaction.domain.usecase.SearchTransactionsUseCase
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<TransactionWithDetails>> = searchQuery
        .flatMapLatest { query ->
            searchTransactionsUseCase(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Rebuild index on init to sync with historical transactions
        viewModelScope.launch {
            try {
                transactionRepository.rebuildSearchIndex()
            } catch (e: Exception) {
                // Handle safely
            }
        }
    }

    fun onQueryChanged(query: String) {
        searchQuery.value = query
    }
}
