package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("transaction_id")
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
