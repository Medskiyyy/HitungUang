package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_settings")
data class NotificationSettingEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "daily_reminder_enabled")
    val dailyReminderEnabled: Boolean,
    
    @ColumnInfo(name = "daily_reminder_time")
    val dailyReminderTime: String?,
    
    @ColumnInfo(name = "weekly_review_enabled")
    val weeklyReviewEnabled: Boolean,
    
    @ColumnInfo(name = "monthly_review_enabled")
    val monthlyReviewEnabled: Boolean,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
