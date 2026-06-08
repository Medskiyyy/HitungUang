package com.hitunguang.feature.receipt.domain.model

data class Receipt(
    val id: String,
    val imagePath: String,
    val merchantName: String?,
    val receiptDate: Long?,
    val subtotal: Long?,
    val tax: Long?,
    val total: Long,
    val ocrRawText: String?,
    val createdAt: Long
)

data class ReceiptItem(
    val id: String,
    val receiptId: String,
    val itemName: String,
    val quantity: Double?,
    val unitPrice: Long,
    val subtotal: Long,
    val createdAt: Long
)
