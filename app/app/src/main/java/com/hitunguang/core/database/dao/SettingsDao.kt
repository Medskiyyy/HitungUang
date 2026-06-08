package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hitunguang.core.database.entity.AppSettingEntity
import com.hitunguang.core.database.entity.BackupSettingEntity
import com.hitunguang.core.database.entity.NotificationSettingEntity
import com.hitunguang.core.database.entity.SecuritySettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings LIMIT 1")
    fun getAppSettings(): Flow<AppSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppSettings(settings: AppSettingEntity)

    @Query("SELECT * FROM security_settings LIMIT 1")
    fun getSecuritySettings(): Flow<SecuritySettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSecuritySettings(settings: SecuritySettingEntity)

    @Query("SELECT * FROM notification_settings LIMIT 1")
    fun getNotificationSettings(): Flow<NotificationSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNotificationSettings(settings: NotificationSettingEntity)

    @Query("SELECT * FROM backup_settings LIMIT 1")
    fun getBackupSettings(): Flow<BackupSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBackupSettings(settings: BackupSettingEntity)
}
