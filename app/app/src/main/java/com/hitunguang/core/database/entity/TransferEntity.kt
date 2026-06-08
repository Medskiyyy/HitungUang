package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("from_account_id"),
        Index("to_account_id"),
        Index("transfer_date")
    ]
)
data class TransferEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "from_account_id")
    val fromAccountId: String,
    
    @ColumnInfo(name = "to_account_id")
    val toAccountId: String,
    
    val amount: Long,
    
    @ColumnInfo(name = "admin_fee")
    val adminFee: Long,
    
    val note: String?,
    
    @ColumnInfo(name = "transfer_date")
    val transferDate: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
