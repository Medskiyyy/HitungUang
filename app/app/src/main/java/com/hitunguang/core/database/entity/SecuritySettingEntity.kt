package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_settings")
data class SecuritySettingEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "pin_hash")
    val pinHash: String?,
    
    @ColumnInfo(name = "biometric_enabled")
    val biometricEnabled: Boolean,
    
    @ColumnInfo(name = "recovery_code_hash")
    val recoveryCodeHash: String?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
