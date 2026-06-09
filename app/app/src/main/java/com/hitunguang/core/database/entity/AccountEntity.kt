package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    @ColumnInfo(name = "account_type")
    val accountType: String,
    
    val icon: String?,
    
    @ColumnInfo(name = "initial_balance")
    val initialBalance: Long,
    
    @ColumnInfo(name = "current_balance")
    val currentBalance: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
