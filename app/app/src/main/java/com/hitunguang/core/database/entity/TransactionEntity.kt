package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("account_id"),
        Index("category_id"),
        Index("receipt_id"),
        Index("transaction_date"),
        Index("transaction_type"),
        Index("is_deleted")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "account_id")
    val accountId: String,
    
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    
    @ColumnInfo(name = "receipt_id")
    val receiptId: String?,
    
    @ColumnInfo(name = "transaction_type")
    val transactionType: String,
    
    val title: String,
    
    val note: String?,
    
    val amount: Long,
    
    @ColumnInfo(name = "transaction_date")
    val transactionDate: Long,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean,
    
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
