package com.hitunguang.feature.receipt.presentation

import android.net.Uri

data class ReceiptScannerUiState(
    val imageUri: Uri? = null,
    val isScanning: Boolean = false,
    val rawText: String? = null,
    val errorMessage: String? = null
)
