package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "theme_mode")
    val themeMode: String,
    
    @ColumnInfo(name = "hide_balance")
    val hideBalance: Boolean,
    
    @ColumnInfo(name = "receipt_auto_delete_days")
    val receiptAutoDeleteDays: Int,
    
    @ColumnInfo(name = "dashboard_period")
    val dashboardPeriod: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
