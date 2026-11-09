package com.hitunguang.feature.recyclebin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.recyclebin.domain.usecase.GetDeletedItemsUseCase
import com.hitunguang.feature.recyclebin.domain.usecase.RestoreItemUseCase
import com.hitunguang.feature.recyclebin.domain.usecase.PermanentDeleteItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val getDeletedItemsUseCase: GetDeletedItemsUseCase,
    private val restoreItemUseCase: RestoreItemUseCase,
    private val permanentDeleteItemUseCase: PermanentDeleteItemUseCase
) : ViewModel() {

    val uiState: StateFlow<RecycleBinUiState> = getDeletedItemsUseCase()
        .map { items ->
            RecycleBinUiState(isLoading = false, items = items)
        }
        .catch { e ->
            emit(RecycleBinUiState(error = e.localizedMessage ?: "Ada yang gagal. Coba lagi, ya."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecycleBinUiState(isLoading = true)
        )

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun restoreItem(entityId: String, entityType: String) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                restoreItemUseCase(entityId, entityType)
            } catch (e: Exception) {
                // Safely handle error
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun permanentDeleteItem(entityId: String, entityType: String) {
        if (_isProcessing.value) return
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                permanentDeleteItemUseCase(entityId, entityType)
            } catch (e: Exception) {
                // Safely handle error
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun restoreTransaction(transactionId: String) {
        restoreItem(transactionId, "TRANSACTION")
    }

    fun permanentDeleteTransaction(transactionId: String) {
        permanentDeleteItem(transactionId, "TRANSACTION")
    }
}
