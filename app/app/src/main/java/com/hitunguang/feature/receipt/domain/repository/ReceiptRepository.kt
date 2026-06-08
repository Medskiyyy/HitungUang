package com.hitunguang.feature.receipt.domain.repository

import com.hitunguang.feature.receipt.domain.model.Receipt
import com.hitunguang.feature.receipt.domain.model.ReceiptItem
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    fun getAllReceipts(): Flow<List<Receipt>>
    fun getReceiptById(id: String): Flow<Receipt?>
    fun getReceiptItems(receiptId: String): Flow<List<ReceiptItem>>
    suspend fun saveReceipt(receipt: Receipt, items: List<ReceiptItem>)
    suspend fun deleteReceipt(receipt: Receipt)
}
