package com.hitunguang.feature.receipt.presentation

import com.hitunguang.R
import androidx.annotation.StringRes
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.core.filemanager.ReceiptFileManager
import com.hitunguang.feature.receipt.domain.usecase.ScanReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptScannerViewModel @Inject constructor(
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val receiptFileManager: ReceiptFileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScannerUiState())
    val uiState: StateFlow<ReceiptScannerUiState> = _uiState.asStateFlow()

    fun setImageUri(uri: Uri) {
        _uiState.update {
            it.copy(
                imageUri = uri,
                rawText = null,
                errorMessage = null,
                errorMessageRes = null
            )
        }
    }

    /** Photo-picker URIs expire, so the picked image is copied into the cache first. */
    fun setPickedImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                setImageUri(receiptFileManager.copyToCache(uri))
            } catch (e: Exception) {
                setErrorMessage(R.string.receipt_scanner_error_gallery_read)
            }
        }
    }

    fun discardTempPhoto(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) { receiptFileManager.deleteTempFileIfNeeded(uri) }
    }

    fun deleteStaleTempFiles() {
        viewModelScope.launch { receiptFileManager.deleteStaleTempFiles() }
    }

    fun acceptLowQuality() {
        _uiState.update { it.copy(isLowQualityAccepted = true) }
    }

    /** Clears the scan and deletes its temp photo; use [clearAll] once the photo was handed on. */
    fun reset() {
        _uiState.value.imageUri?.let { discardTempPhoto(it) }
        clearAll()
    }

    fun setErrorMessage(@StringRes message: Int) {
        _uiState.update {
            it.copy(errorMessage = null, errorMessageRes = message)
        }
    }

    fun startScan() {
        val uri = _uiState.value.imageUri ?: return
        _uiState.update {
            it.copy(
                isScanning = true,
                errorMessage = null,
                errorMessageRes = null,
                rawText = null,
                scanQuality = ScanQuality.UNKNOWN,
                isLowQualityAccepted = false
            )
        }

        viewModelScope.launch {
            scanReceiptUseCase(uri)
                .onSuccess { result ->
                    val quality = evaluateScanQuality(result.text, result.averageConfidence)
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            rawText = result.text,
                            scanQuality = quality
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = error.localizedMessage,
                            errorMessageRes = if (error.localizedMessage == null) R.string.receipt_scanner_error_scan_failed else null
                        )
                    }
                }
        }
    }

    fun clearAll() {
        _uiState.value = ReceiptScannerUiState()
    }

    /**
     * ML Kit's on-device recognizer reports no confidence score, so quality is derived
     * from the shape of the extracted text: a usable receipt scan yields several lines
     * containing digits. A blurred or dark photo yields little or no text.
     */
    private fun evaluateScanQuality(text: String, averageConfidence: Float?): ScanQuality {
        if (averageConfidence != null) {
            return if (averageConfidence >= 0.80f) ScanQuality.GOOD else ScanQuality.LOW
        }
        if (text.isBlank()) return ScanQuality.LOW

        val lines = text.lines().filter { it.isNotBlank() }
        val hasAmountLike = Regex("\\d[\\d.,]{2,}").containsMatchIn(text)

        return if (lines.size >= MIN_QUALITY_LINES && text.length >= MIN_QUALITY_CHARS && hasAmountLike) {
            ScanQuality.GOOD
        } else {
            ScanQuality.LOW
        }
    }

    private companion object {
        const val MIN_QUALITY_LINES = 4
        const val MIN_QUALITY_CHARS = 40
    }
}
