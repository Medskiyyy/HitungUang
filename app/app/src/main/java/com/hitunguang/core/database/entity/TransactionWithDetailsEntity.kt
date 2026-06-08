package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class TransactionWithDetailsEntity(
    @Embedded
    val transaction: TransactionEntity,
    
    @ColumnInfo(name = "account_name")
    val accountName: String,
    
    @ColumnInfo(name = "category_name")
    val categoryName: String?
)
