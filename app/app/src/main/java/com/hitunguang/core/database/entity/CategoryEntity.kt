package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    @ColumnInfo(name = "category_type")
    val categoryType: String,
    
    val icon: String?,
    
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
