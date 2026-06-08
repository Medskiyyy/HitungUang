package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recycle_bin",
    indices = [
        Index("entity_id"),
        Index("deleted_at"),
        Index("expire_at")
    ]
)
data class RecycleBinEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long,
    
    @ColumnInfo(name = "expire_at")
    val expireAt: Long
)
