package com.hitunguang.feature.receipt.presentation

import androidx.annotation.StringRes
import android.net.Uri

data class ReceiptScannerUiState(
    val imageUri: Uri? = null,
    val isScanning: Boolean = false,
    val rawText: String? = null,
    val scanQuality: ScanQuality = ScanQuality.UNKNOWN,
    /** The user chose to continue with a LOW quality scan instead of retaking it. */
    val isLowQualityAccepted: Boolean = false,
    val errorMessage: String? = null,
    @StringRes val errorMessageRes: Int? = null
)

enum class ScanQuality {
    GOOD,      // Text looks like a readable receipt
    LOW,       // Little or no usable text - suggest retaking the photo
    UNKNOWN    // Not scanned yet
}
