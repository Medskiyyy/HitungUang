package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipts",
    indices = [
        Index("receipt_date")
    ]
)
data class ReceiptEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "image_path")
    val imagePath: String,
    
    @ColumnInfo(name = "merchant_name")
    val merchantName: String?,
    
    @ColumnInfo(name = "receipt_date")
    val receiptDate: Long?,
    
    val subtotal: Long?,
    
    val tax: Long?,
    
    val total: Long,
    
    @ColumnInfo(name = "ocr_raw_text")
    val ocrRawText: String?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
