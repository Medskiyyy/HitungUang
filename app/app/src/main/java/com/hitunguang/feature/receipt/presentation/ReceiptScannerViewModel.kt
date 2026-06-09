package com.hitunguang.feature.receipt.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.receipt.domain.usecase.ScanReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptScannerViewModel @Inject constructor(
    private val scanReceiptUseCase: ScanReceiptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScannerUiState())
    val uiState: StateFlow<ReceiptScannerUiState> = _uiState.asStateFlow()

    fun setImageUri(uri: Uri) {
        _uiState.update {
            it.copy(
                imageUri = uri,
                rawText = null,
                errorMessage = null
            )
        }
    }

    fun setErrorMessage(message: String) {
        _uiState.update {
            it.copy(errorMessage = message)
        }
    }

    fun startScan() {
        val uri = _uiState.value.imageUri ?: return
        _uiState.update { it.copy(isScanning = true, errorMessage = null, rawText = null) }

        viewModelScope.launch {
            scanReceiptUseCase(uri)
                .onSuccess { text ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            rawText = text
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = error.localizedMessage ?: "Gagal memindai struk"
                        )
                    }
                }
        }
    }

    fun clearAll() {
        _uiState.value = ReceiptScannerUiState()
    }
}
