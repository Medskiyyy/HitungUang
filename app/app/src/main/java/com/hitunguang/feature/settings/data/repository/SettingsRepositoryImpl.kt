package com.hitunguang.feature.settings.data.repository

import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.model.BackupSettings
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    override fun getAppSettings(): Flow<AppSettings?> {
        return settingsDataStore.appSettings
    }

    override suspend fun saveAppSettings(settings: AppSettings) {
        settingsDataStore.saveAppSettings(settings)
    }

    override fun getSecuritySettings(): Flow<SecuritySettings?> {
        return settingsDataStore.securitySettings
    }

    override suspend fun saveSecuritySettings(settings: SecuritySettings) {
        settingsDataStore.saveSecuritySettings(settings)
    }

    override fun getNotificationSettings(): Flow<NotificationSettings?> {
        return settingsDataStore.notificationSettings
    }

    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        settingsDataStore.saveNotificationSettings(settings)
    }

    override fun getBackupSettings(): Flow<BackupSettings?> {
        return settingsDataStore.backupSettings
    }

    override suspend fun saveBackupSettings(settings: BackupSettings) {
        settingsDataStore.saveBackupSettings(settings)
    }
}
