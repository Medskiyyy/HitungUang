package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_settings")
data class BackupSettingEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "backup_folder_uri")
    val backupFolderUri: String?,
    
    @ColumnInfo(name = "backup_frequency")
    val backupFrequency: String,
    
    @ColumnInfo(name = "auto_backup_enabled")
    val autoBackupEnabled: Boolean,
    
    @ColumnInfo(name = "last_backup_at")
    val lastBackupAt: Long?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
