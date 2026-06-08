package com.hitunguang.core.ocr

data class ParsedReceipt(
    val merchantName: String?,
    val date: Long?,
    val subtotal: Long?,
    val tax: Long?,
    val total: Long,
    val items: List<ParsedReceiptItem>
)

data class ParsedReceiptItem(
    val name: String,
    val quantity: Double?,
    val unitPrice: Long,
    val subtotal: Long
)
