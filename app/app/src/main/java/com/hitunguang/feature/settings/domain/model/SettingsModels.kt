package com.hitunguang.feature.settings.domain.model

data class AppSettings(
    val id: String,
    val themeMode: String,
    val hideBalance: Boolean,
    val receiptAutoDeleteDays: Int,
    val dashboardPeriod: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class BackupSettings(
    val id: String,
    val backupFolderUri: String?,
    val backupFrequency: String,
    val autoBackupEnabled: Boolean,
    val lastBackupAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

data class NotificationSettings(
    val id: String,
    val dailyReminderEnabled: Boolean,
    val dailyReminderTime: String?,
    val weeklyReviewEnabled: Boolean,
    val monthlyReviewEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class SecuritySettings(
    val id: String,
    val pinHash: String?,
    val biometricEnabled: Boolean,
    val recoveryCodeHash: String?,
    val createdAt: Long,
    val updatedAt: Long
)
