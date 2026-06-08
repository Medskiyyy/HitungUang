package com.hitunguang.feature.settings.data.mapper

import com.hitunguang.core.database.entity.AppSettingEntity
import com.hitunguang.core.database.entity.BackupSettingEntity
import com.hitunguang.core.database.entity.NotificationSettingEntity
import com.hitunguang.core.database.entity.SecuritySettingEntity
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.model.BackupSettings
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import com.hitunguang.feature.settings.domain.model.SecuritySettings

object SettingsMapper {
    fun toDomain(entity: AppSettingEntity): AppSettings {
        return AppSettings(
            id = entity.id,
            themeMode = entity.themeMode,
            hideBalance = entity.hideBalance,
            receiptAutoDeleteDays = entity.receiptAutoDeleteDays,
            dashboardPeriod = entity.dashboardPeriod,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: AppSettings): AppSettingEntity {
        return AppSettingEntity(
            id = domain.id,
            themeMode = domain.themeMode,
            hideBalance = domain.hideBalance,
            receiptAutoDeleteDays = domain.receiptAutoDeleteDays,
            dashboardPeriod = domain.dashboardPeriod,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun toDomain(entity: BackupSettingEntity): BackupSettings {
        return BackupSettings(
            id = entity.id,
            backupFolderUri = entity.backupFolderUri,
            backupFrequency = entity.backupFrequency,
            autoBackupEnabled = domainActive(entity.autoBackupEnabled),
            lastBackupAt = entity.lastBackupAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun domainActive(value: Boolean): Boolean = value

    fun toEntity(domain: BackupSettings): BackupSettingEntity {
        return BackupSettingEntity(
            id = domain.id,
            backupFolderUri = domain.backupFolderUri,
            backupFrequency = domain.backupFrequency,
            autoBackupEnabled = domain.autoBackupEnabled,
            lastBackupAt = domain.lastBackupAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun toDomain(entity: NotificationSettingEntity): NotificationSettings {
        return NotificationSettings(
            id = entity.id,
            dailyReminderEnabled = entity.dailyReminderEnabled,
            dailyReminderTime = entity.dailyReminderTime,
            weeklyReviewEnabled = entity.weeklyReviewEnabled,
            monthlyReviewEnabled = entity.monthlyReviewEnabled,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: NotificationSettings): NotificationSettingEntity {
        return NotificationSettingEntity(
            id = domain.id,
            dailyReminderEnabled = domain.dailyReminderEnabled,
            dailyReminderTime = domain.dailyReminderTime,
            weeklyReviewEnabled = domain.weeklyReviewEnabled,
            monthlyReviewEnabled = domain.monthlyReviewEnabled,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun toDomain(entity: SecuritySettingEntity): SecuritySettings {
        return SecuritySettings(
            id = entity.id,
            pinHash = entity.pinHash,
            biometricEnabled = entity.biometricEnabled,
            recoveryCodeHash = entity.recoveryCodeHash,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: SecuritySettings): SecuritySettingEntity {
        return SecuritySettingEntity(
            id = domain.id,
            pinHash = domain.pinHash,
            biometricEnabled = domain.biometricEnabled,
            recoveryCodeHash = domain.recoveryCodeHash,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
