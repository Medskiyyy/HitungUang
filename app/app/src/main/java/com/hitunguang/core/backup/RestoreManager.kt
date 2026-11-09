package com.hitunguang.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.settings.domain.model.AppSettings
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
    private val settingsDataStore: SettingsDataStore,
    private val database: HitungUangDatabase
) {
    /**
     * Restores the app from a ZIP backup file pointed to by [zipUri].
     * Steps:
     * 1. Validate the ZIP has a database entry.
     * 2. Extract ZIP to a staging directory (zip-slip protected).
     * 3. Validate the extracted database before touching live data.
     * 4. Close Room, back up live data to a rollback dir, swap files (rollback on failure).
     * 5. Restore non-security settings from settings.json.
     * 6. Restart the app.
     */
    suspend fun restore(zipUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "restore_staging")
        val rollbackDir = File(context.cacheDir, "restore_rollback")
        try {
            val valid = context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipHelper.containsEntry(inputStream, BackupManager.DB_ENTRY)
            } ?: false

            if (!valid) {
                return@withContext Result.failure(
                    IllegalArgumentException("File yang dipilih bukan file cadangan HitungUang yang benar.")
                )
            }

            stagingDir.deleteRecursively()
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipHelper.extractZip(inputStream, stagingDir)
            }

            val stagedDb = File(stagingDir, BackupManager.DB_ENTRY)
            File(stagedDb.parentFile, "${RestoreFiles.DB_NAME}-wal").delete()
            File(stagedDb.parentFile, "${RestoreFiles.DB_NAME}-shm").delete()
            RestoreFiles.validateDatabase(stagedDb)

            database.close()
            val dbDir = context.getDatabasePath(RestoreFiles.DB_NAME).parentFile!!
            RestoreFiles.swap(stagingDir, dbDir, context.filesDir, rollbackDir)

            val settingsJsonFile = File(stagingDir, "settings.json")
            if (settingsJsonFile.exists()) {
                restoreSettingsFromJson(settingsJsonFile.readText())
            }

            stagingDir.deleteRecursively()
            rollbackDir.deleteRecursively()
            Timber.i("Restore berhasil. Memulai ulang aplikasi...")
            restartApp()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Restore gagal")
            Result.failure(e)
        } finally {
            stagingDir.deleteRecursively()
            rollbackDir.deleteRecursively()
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
