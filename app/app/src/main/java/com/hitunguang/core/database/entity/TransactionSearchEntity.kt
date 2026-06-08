package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "transaction_search")
data class TransactionSearchEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int? = null,

    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "transaction_title")
    val transactionTitle: String,
    
    @ColumnInfo(name = "transaction_note")
    val transactionNote: String?,
    
    @ColumnInfo(name = "category_name")
    val categoryName: String?,
    
    @ColumnInfo(name = "receipt_item_name")
    val receiptItemName: String?
)
