package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_drafts")
data class TransactionDraftEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "transaction_type")
    val transactionType: String?,
    
    @ColumnInfo(name = "account_id")
    val accountId: String?,
    
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    
    val title: String?,
    
    val note: String?,
    
    val amount: Long?,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
