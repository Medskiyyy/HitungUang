package com.hitunguang.feature.receipt.presentation

import android.net.Uri

data class ReceiptScannerUiState(
    val imageUri: Uri? = null,
    val isScanning: Boolean = false,
    val rawText: String? = null,
    val scanQuality: ScanQuality = ScanQuality.UNKNOWN,
    val errorMessage: String? = null
)

enum class ScanQuality {
    GOOD,      // Text looks like a readable receipt
    LOW,       // Little or no usable text - suggest retaking the photo
    UNKNOWN    // Not scanned yet
}
