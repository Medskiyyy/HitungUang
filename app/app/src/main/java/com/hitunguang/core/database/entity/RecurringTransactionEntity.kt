package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
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
        Index("category_id")
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "account_id")
    val accountId: String,
    
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    
    @ColumnInfo(name = "transaction_type")
    val transactionType: String,
    
    val title: String,
    
    val note: String?,
    
    val amount: Long,
    
    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String,
    
    @ColumnInfo(name = "next_run_at")
    val nextRunAt: Long,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
