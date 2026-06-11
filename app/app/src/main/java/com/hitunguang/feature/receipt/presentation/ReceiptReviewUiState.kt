package com.hitunguang.feature.receipt.presentation

import android.net.Uri
import com.hitunguang.feature.receipt.domain.usecase.ParsedItemInput

data class ReceiptReviewUiState(
    val imageUri: Uri? = null,
    val merchantName: String = "",
    val receiptDate: Long = System.currentTimeMillis(),
    val accountId: String = "",
    val categoryId: String? = null,
    val items: List<ParsedItemInput> = emptyList(),
    val taxStr: String = "0",
    val subtotal: Long = 0L,
    val totalStr: String = "0",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val isMerchantConfident: Boolean = false,
    val isDateConfident: Boolean = false,
    val isItemsConfident: Boolean = false
)
