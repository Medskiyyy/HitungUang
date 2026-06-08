package com.hitunguang.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.model.BackupSettings
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreManager @Inject constructor(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    /**
     * Restores the app from a ZIP backup file pointed to by [zipUri].
     * Steps:
     * 1. Validate the ZIP has a database entry.
     * 2. Extract ZIP to a temp directory.
     * 3. Copy DB files to the databases directory.
     * 4. Copy attachments to filesDir/attachments.
     * 5. Restore non-security settings from settings.json.
     * 6. Restart the app.
     */
    suspend fun restore(zipUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 1. Validate: check the ZIP contains the database entry
            val valid = context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipHelper.containsEntry(inputStream, BackupManager.DB_ENTRY)
            } ?: false

            if (!valid) {
                return@withContext Result.failure(
                    IllegalArgumentException("File yang dipilih bukan file backup HitungUang yang valid.")
                )
            }

            // 2. Extract to temp directory
            val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipHelper.extractZip(inputStream, tempDir)
            }

            // 3. Copy database files
            val dbDir = context.getDatabasePath("hitunguang.db").parentFile
            dbDir?.mkdirs()

            val tempDbDir = File(tempDir, "database")
            listOf("hitunguang.db", "hitunguang.db-wal", "hitunguang.db-shm").forEach { dbFileName ->
                val src = File(tempDbDir, dbFileName)
                if (src.exists()) {
                    src.copyTo(File(dbDir, dbFileName), overwrite = true)
                }
            }

            // 4. Copy attachments
            val tempAttachments = File(tempDir, "attachments")
            if (tempAttachments.exists() && tempAttachments.isDirectory) {
                val targetAttachments = File(context.filesDir, "attachments")
                targetAttachments.deleteRecursively()
                tempAttachments.copyRecursively(targetAttachments, overwrite = true)
            }

            // 5. Restore settings from settings.json (excluding SecuritySettings)
            val settingsJsonFile = File(tempDir, "settings.json")
            if (settingsJsonFile.exists()) {
                restoreSettingsFromJson(settingsJsonFile.readText())
            }

            // 6. Clean up temp directory
            tempDir.deleteRecursively()

            Timber.i("Restore berhasil. Memulai ulang aplikasi...")

            // 7. Restart the app
            restartApp()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Restore gagal")
            Result.failure(e)
        }
    }

    private suspend fun restoreSettingsFromJson(json: String) {
        try {
            val root = JSONObject(json)
            val now = System.currentTimeMillis()

            // Restore AppSettings
            if (root.has("appSettings")) {
                val obj = root.getJSONObject("appSettings")
                val existingApp = settingsDataStore.appSettings.firstOrNull()
                settingsDataStore.saveAppSettings(
                    AppSettings(
                        id = existingApp?.id ?: "app_settings",
                        themeMode = obj.optString("themeMode", "SYSTEM"),
                        hideBalance = obj.optBoolean("hideBalance", false),
                        receiptAutoDeleteDays = obj.optInt("receiptAutoDeleteDays", 30),
                        dashboardPeriod = obj.optString("dashboardPeriod", "MONTHLY"),
                        createdAt = existingApp?.createdAt ?: now,
                        updatedAt = now
                    )
                )
            }

            // Restore BackupSettings
            if (root.has("backupSettings")) {
                val obj = root.getJSONObject("backupSettings")
                val existingBackup = settingsDataStore.backupSettings.firstOrNull()
                settingsDataStore.saveBackupSettings(
                    BackupSettings(
                        id = existingBackup?.id ?: "backup_settings",
                        backupFolderUri = if (obj.isNull("backupFolderUri")) null else obj.optString("backupFolderUri"),
                        backupFrequency = obj.optString("backupFrequency", "WEEKLY"),
                        autoBackupEnabled = obj.optBoolean("autoBackupEnabled", false),
                        lastBackupAt = null,
                        createdAt = existingBackup?.createdAt ?: now,
                        updatedAt = now
                    )
                )
            }

            // Restore NotificationSettings
            if (root.has("notificationSettings")) {
                val obj = root.getJSONObject("notificationSettings")
                val existingNotif = settingsDataStore.notificationSettings.firstOrNull()
                settingsDataStore.saveNotificationSettings(
                    NotificationSettings(
                        id = existingNotif?.id ?: "notification_settings",
                        dailyReminderEnabled = obj.optBoolean("dailyReminderEnabled", false),
                        dailyReminderTime = if (obj.isNull("dailyReminderTime")) null else obj.optString("dailyReminderTime"),
                        weeklyReviewEnabled = obj.optBoolean("weeklyReviewEnabled", false),
                        monthlyReviewEnabled = obj.optBoolean("monthlyReviewEnabled", false),
                        createdAt = existingNotif?.createdAt ?: now,
                        updatedAt = now
                    )
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Gagal memulihkan settings dari JSON — settings lama tetap digunakan")
        }
    }

    private fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(it)
        }
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
