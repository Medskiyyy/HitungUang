package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("category_id"),
        Index("is_active"),
        Index("start_date")
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    
    @ColumnInfo(name = "budget_type")
    val budgetType: String,
    
    val amount: Long,
    
    @ColumnInfo(name = "threshold_percent")
    val thresholdPercent: Int,
    
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    
    @ColumnInfo(name = "end_date")
    val endDate: Long,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
