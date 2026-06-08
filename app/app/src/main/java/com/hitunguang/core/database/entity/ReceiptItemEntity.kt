package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receipt_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("receipt_id")
    ]
)
data class ReceiptItemEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "receipt_id")
    val receiptId: String,
    
    @ColumnInfo(name = "item_name")
    val itemName: String,
    
    val quantity: Double?,
    
    @ColumnInfo(name = "unit_price")
    val unitPrice: Long,
    
    val subtotal: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
