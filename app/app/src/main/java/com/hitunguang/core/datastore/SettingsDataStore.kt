package com.hitunguang.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.model.BackupSettings
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.transaction.domain.model.TransactionDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // App Settings keys
        private val KEY_APP_SETTINGS_ID = stringPreferencesKey("app_settings_id")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_HIDE_BALANCE = booleanPreferencesKey("hide_balance")
        private val KEY_RECEIPT_AUTO_DELETE_DAYS = intPreferencesKey("receipt_auto_delete_days")
        private val KEY_DASHBOARD_PERIOD = stringPreferencesKey("dashboard_period")
        private val KEY_APP_SETTINGS_CREATED_AT = longPreferencesKey("app_settings_created_at")
        private val KEY_APP_SETTINGS_UPDATED_AT = longPreferencesKey("app_settings_updated_at")

        // Backup Settings keys
        private val KEY_BACKUP_SETTINGS_ID = stringPreferencesKey("backup_settings_id")
        private val KEY_BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        private val KEY_BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        private val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        private val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        private val KEY_BACKUP_SETTINGS_CREATED_AT = longPreferencesKey("backup_settings_created_at")
        private val KEY_BACKUP_SETTINGS_UPDATED_AT = longPreferencesKey("backup_settings_updated_at")

        // Notification Settings keys
        private val KEY_NOTIFICATION_SETTINGS_ID = stringPreferencesKey("notification_settings_id")
        private val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        private val KEY_DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
        private val KEY_WEEKLY_REVIEW_ENABLED = booleanPreferencesKey("weekly_review_enabled")
        private val KEY_MONTHLY_REVIEW_ENABLED = booleanPreferencesKey("monthly_review_enabled")
        private val KEY_NOTIFICATION_SETTINGS_CREATED_AT = longPreferencesKey("notification_settings_created_at")
        private val KEY_NOTIFICATION_SETTINGS_UPDATED_AT = longPreferencesKey("notification_settings_updated_at")

        // Security Settings keys
        private val KEY_SECURITY_SETTINGS_ID = stringPreferencesKey("security_settings_id")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_RECOVERY_CODE_HASH = stringPreferencesKey("recovery_code_hash")
        private val KEY_SECURITY_SETTINGS_CREATED_AT = longPreferencesKey("security_settings_created_at")
        private val KEY_SECURITY_SETTINGS_UPDATED_AT = longPreferencesKey("security_settings_updated_at")

        // Draft Transaction key
        private val KEY_TRANSACTION_DRAFT = stringPreferencesKey("transaction_draft")
    }

    val appSettings: Flow<AppSettings?> = dataStore.data.map { preferences ->
        val theme = preferences[KEY_THEME_MODE] ?: return@map null
        AppSettings(
            id = preferences[KEY_APP_SETTINGS_ID] ?: "default",
            themeMode = theme,
            hideBalance = preferences[KEY_HIDE_BALANCE] ?: false,
            receiptAutoDeleteDays = preferences[KEY_RECEIPT_AUTO_DELETE_DAYS] ?: 30,
            dashboardPeriod = preferences[KEY_DASHBOARD_PERIOD] ?: "MONTHLY",
            createdAt = preferences[KEY_APP_SETTINGS_CREATED_AT] ?: System.currentTimeMillis(),
            updatedAt = preferences[KEY_APP_SETTINGS_UPDATED_AT] ?: System.currentTimeMillis()
        )
    }

    suspend fun saveAppSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_SETTINGS_ID] = settings.id
            preferences[KEY_THEME_MODE] = settings.themeMode
            preferences[KEY_HIDE_BALANCE] = settings.hideBalance
            preferences[KEY_RECEIPT_AUTO_DELETE_DAYS] = settings.receiptAutoDeleteDays
            preferences[KEY_DASHBOARD_PERIOD] = settings.dashboardPeriod
            preferences[KEY_APP_SETTINGS_CREATED_AT] = settings.createdAt
            preferences[KEY_APP_SETTINGS_UPDATED_AT] = settings.updatedAt
        }
    }

    val backupSettings: Flow<BackupSettings?> = dataStore.data.map { preferences ->
        val frequency = preferences[KEY_BACKUP_FREQUENCY] ?: return@map null
        BackupSettings(
            id = preferences[KEY_BACKUP_SETTINGS_ID] ?: "default",
            backupFolderUri = preferences[KEY_BACKUP_FOLDER_URI],
            backupFrequency = frequency,
            autoBackupEnabled = preferences[KEY_AUTO_BACKUP_ENABLED] ?: false,
            lastBackupAt = preferences[KEY_LAST_BACKUP_AT],
            createdAt = preferences[KEY_BACKUP_SETTINGS_CREATED_AT] ?: System.currentTimeMillis(),
            updatedAt = preferences[KEY_BACKUP_SETTINGS_UPDATED_AT] ?: System.currentTimeMillis()
        )
    }

    suspend fun saveBackupSettings(settings: BackupSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_BACKUP_SETTINGS_ID] = settings.id
            if (settings.backupFolderUri != null) {
                preferences[KEY_BACKUP_FOLDER_URI] = settings.backupFolderUri
            } else {
                preferences.remove(KEY_BACKUP_FOLDER_URI)
            }
            preferences[KEY_BACKUP_FREQUENCY] = settings.backupFrequency
            preferences[KEY_AUTO_BACKUP_ENABLED] = settings.autoBackupEnabled
            if (settings.lastBackupAt != null) {
                preferences[KEY_LAST_BACKUP_AT] = settings.lastBackupAt
            } else {
                preferences.remove(KEY_LAST_BACKUP_AT)
            }
            preferences[KEY_BACKUP_SETTINGS_CREATED_AT] = settings.createdAt
            preferences[KEY_BACKUP_SETTINGS_UPDATED_AT] = settings.updatedAt
        }
    }

    val notificationSettings: Flow<NotificationSettings?> = dataStore.data.map { preferences ->
        val id = preferences[KEY_NOTIFICATION_SETTINGS_ID] ?: return@map null
        NotificationSettings(
            id = id,
            dailyReminderEnabled = preferences[KEY_DAILY_REMINDER_ENABLED] ?: false,
            dailyReminderTime = preferences[KEY_DAILY_REMINDER_TIME],
            weeklyReviewEnabled = preferences[KEY_WEEKLY_REVIEW_ENABLED] ?: false,
            monthlyReviewEnabled = preferences[KEY_MONTHLY_REVIEW_ENABLED] ?: false,
            createdAt = preferences[KEY_NOTIFICATION_SETTINGS_CREATED_AT] ?: System.currentTimeMillis(),
            updatedAt = preferences[KEY_NOTIFICATION_SETTINGS_UPDATED_AT] ?: System.currentTimeMillis()
        )
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_SETTINGS_ID] = settings.id
            preferences[KEY_DAILY_REMINDER_ENABLED] = settings.dailyReminderEnabled
            if (settings.dailyReminderTime != null) {
                preferences[KEY_DAILY_REMINDER_TIME] = settings.dailyReminderTime
            } else {
                preferences.remove(KEY_DAILY_REMINDER_TIME)
            }
            preferences[KEY_WEEKLY_REVIEW_ENABLED] = settings.weeklyReviewEnabled
            preferences[KEY_MONTHLY_REVIEW_ENABLED] = settings.monthlyReviewEnabled
            preferences[KEY_NOTIFICATION_SETTINGS_CREATED_AT] = settings.createdAt
            preferences[KEY_NOTIFICATION_SETTINGS_UPDATED_AT] = settings.updatedAt
        }
    }

    val securitySettings: Flow<SecuritySettings?> = dataStore.data.map { preferences ->
        val id = preferences[KEY_SECURITY_SETTINGS_ID] ?: return@map null
        SecuritySettings(
            id = id,
            pinHash = preferences[KEY_PIN_HASH],
            biometricEnabled = preferences[KEY_BIOMETRIC_ENABLED] ?: false,
            recoveryCodeHash = preferences[KEY_RECOVERY_CODE_HASH],
            createdAt = preferences[KEY_SECURITY_SETTINGS_CREATED_AT] ?: System.currentTimeMillis(),
            updatedAt = preferences[KEY_SECURITY_SETTINGS_UPDATED_AT] ?: System.currentTimeMillis()
        )
    }

    suspend fun saveSecuritySettings(settings: SecuritySettings) {
        dataStore.edit { preferences ->
            preferences[KEY_SECURITY_SETTINGS_ID] = settings.id
            if (settings.pinHash != null) {
                preferences[KEY_PIN_HASH] = settings.pinHash
            } else {
                preferences.remove(KEY_PIN_HASH)
            }
            preferences[KEY_BIOMETRIC_ENABLED] = settings.biometricEnabled
            if (settings.recoveryCodeHash != null) {
                preferences[KEY_RECOVERY_CODE_HASH] = settings.recoveryCodeHash
            } else {
                preferences.remove(KEY_RECOVERY_CODE_HASH)
            }
            preferences[KEY_SECURITY_SETTINGS_CREATED_AT] = settings.createdAt
            preferences[KEY_SECURITY_SETTINGS_UPDATED_AT] = settings.updatedAt
        }
    }

    val transactionDraft: Flow<TransactionDraft?> = dataStore.data.map { preferences ->
        val jsonStr = preferences[KEY_TRANSACTION_DRAFT] ?: return@map null
        try {
            Json.decodeFromString<TransactionDraft>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveTransactionDraft(draft: TransactionDraft) {
        val jsonStr = Json.encodeToString(TransactionDraft.serializer(), draft)
        dataStore.edit { preferences ->
            preferences[KEY_TRANSACTION_DRAFT] = jsonStr
        }
    }

    suspend fun clearTransactionDraft() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_TRANSACTION_DRAFT)
        }
    }
}
