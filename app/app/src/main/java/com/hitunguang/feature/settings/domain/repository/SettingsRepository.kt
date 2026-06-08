package com.hitunguang.feature.settings.domain.repository

import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.model.BackupSettings
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAppSettings(): Flow<AppSettings?>
    suspend fun saveAppSettings(settings: AppSettings)

    fun getSecuritySettings(): Flow<SecuritySettings?>
    suspend fun saveSecuritySettings(settings: SecuritySettings)

    fun getNotificationSettings(): Flow<NotificationSettings?>
    suspend fun saveNotificationSettings(settings: NotificationSettings)

    fun getBackupSettings(): Flow<BackupSettings?>
    suspend fun saveBackupSettings(settings: BackupSettings)
}
