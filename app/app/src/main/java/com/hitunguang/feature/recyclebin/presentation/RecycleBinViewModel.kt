package com.hitunguang.feature.recyclebin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.recyclebin.domain.usecase.GetDeletedItemsUseCase
import com.hitunguang.feature.recyclebin.domain.usecase.RestoreTransactionUseCase
import com.hitunguang.feature.recyclebin.domain.usecase.PermanentDeleteTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val getDeletedItemsUseCase: GetDeletedItemsUseCase,
    private val restoreTransactionUseCase: RestoreTransactionUseCase,
    private val permanentDeleteTransactionUseCase: PermanentDeleteTransactionUseCase
) : ViewModel() {

    val uiState: StateFlow<RecycleBinUiState> = getDeletedItemsUseCase()
        .map { items ->
            RecycleBinUiState(isLoading = false, items = items)
        }
        .catch { e ->
            emit(RecycleBinUiState(error = e.localizedMessage ?: "Unknown error"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecycleBinUiState(isLoading = true)
        )

    fun restoreTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                restoreTransactionUseCase(transactionId)
            } catch (e: Exception) {
                // Safely handle error
            }
        }
    }

    fun permanentDeleteTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                permanentDeleteTransactionUseCase(transactionId)
            } catch (e: Exception) {
                // Safely handle error
            }
        }
    }
}
