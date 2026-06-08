package com.hitunguang.feature.receipt.presentation

import androidx.lifecycle.ViewModel
import com.hitunguang.feature.receipt.domain.model.Receipt
import com.hitunguang.feature.receipt.domain.model.ReceiptItem
import com.hitunguang.feature.receipt.domain.usecase.GetReceiptByIdUseCase
import com.hitunguang.feature.receipt.domain.usecase.GetReceiptItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    private val getReceiptByIdUseCase: GetReceiptByIdUseCase,
    private val getReceiptItemsUseCase: GetReceiptItemsUseCase
) : ViewModel() {
    fun getReceipt(id: String): Flow<Receipt?> = getReceiptByIdUseCase(id)
    fun getItems(receiptId: String): Flow<List<ReceiptItem>> = getReceiptItemsUseCase(receiptId)
}
