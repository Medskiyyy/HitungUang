package com.hitunguang.feature.transfer.presentation

import com.hitunguang.feature.account.domain.model.Account

data class TransferUiState(
    val fromAccount: Account? = null,
    val toAccount: Account? = null,
    val amount: String = "",
    val adminFee: String = "0",
    val note: String = "",
    val transferDate: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
