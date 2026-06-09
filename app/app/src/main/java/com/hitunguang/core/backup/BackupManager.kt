package com.hitunguang.core.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.settings.domain.model.BackupSettings
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        const val DB_ENTRY = "database/hitunguang.db"
        const val DB_WAL_ENTRY = "database/hitunguang.db-wal"
        const val DB_SHM_ENTRY = "database/hitunguang.db-shm"
        const val ATTACHMENTS_PREFIX = "attachments/"
        const val RECEIPTS_PREFIX = "receipts/"
        const val SETTINGS_ENTRY = "settings.json"
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }

    /**
     * Creates a ZIP backup of the database, attachments, and settings.
     * Writes the ZIP file to the user-configured SAF folder.
     * @return Result.success(filename) on success, Result.failure(exception) on error.
     */
    suspend fun createBackup(folderUri: String): Result<String> {
        return try {
            // 1. Checkpoint WAL so the .db file is up-to-date before copy
            checkpointWal()

            val timestamp = DATE_FORMAT.format(Date())
            val fileName = "hitunguang_backup_$timestamp.zip"

            // 2. Resolve the SAF folder and create the ZIP file
            val treeUri = Uri.parse(folderUri)
            val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val folderDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)

            val zipUri = DocumentsContract.createDocument(
                context.contentResolver,
                folderDocUri,
                "application/zip",
                fileName
            ) ?: return Result.failure(IllegalStateException("Gagal membuat file backup di folder tujuan."))

            context.contentResolver.openOutputStream(zipUri)?.use { outputStream ->
                ZipOutputStream(outputStream.buffered()).use { zip ->
                    // 4. Add database files
                    val dbDir = context.getDatabasePath("hitunguang.db").parentFile
                    ZipHelper.addFileToZip(zip, File(dbDir, "hitunguang.db"), DB_ENTRY)
                    ZipHelper.addFileToZip(zip, File(dbDir, "hitunguang.db-wal"), DB_WAL_ENTRY)
                    ZipHelper.addFileToZip(zip, File(dbDir, "hitunguang.db-shm"), DB_SHM_ENTRY)

                    // 5. Add attachments directory
                    val attachmentsDir = File(context.filesDir, "attachments")
                    ZipHelper.addDirectoryToZip(zip, attachmentsDir, ATTACHMENTS_PREFIX)

                    // Add receipts directory
                    val receiptsDir = File(context.filesDir, "receipts")
                    ZipHelper.addDirectoryToZip(zip, receiptsDir, RECEIPTS_PREFIX)

                    // 6. Export settings.json (exclude SecuritySettings for security)
                    val settingsJson = buildSettingsJson()
                    zip.putNextEntry(java.util.zip.ZipEntry(SETTINGS_ENTRY))
                    zip.write(settingsJson.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            } ?: return Result.failure(IllegalStateException("Tidak dapat membuka output stream ke folder backup."))

            // 7. Update lastBackupAt timestamp
            val currentSettings = settingsDataStore.backupSettings.firstOrNull()
            if (currentSettings != null) {
                settingsDataStore.saveBackupSettings(
                    currentSettings.copy(
                        lastBackupAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            Timber.i("Backup berhasil dibuat: $fileName")
            Result.success(fileName)
        } catch (e: Exception) {
            Timber.e(e, "Backup gagal")
            Result.failure(e)
        }
    }

    private fun checkpointWal() {
        try {
            val db = context.getDatabasePath("hitunguang.db")
            if (!db.exists()) return
            // Use SQLiteDatabase API to issue WAL checkpoint
            val sqliteDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                db.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
            )
            sqliteDb.use { it.execSQL("PRAGMA wal_checkpoint(FULL)") }
        } catch (e: Exception) {
            Timber.w(e, "WAL checkpoint gagal — backup tetap dilanjutkan")
        }
    }

    private suspend fun buildSettingsJson(): String {
        val sb = StringBuilder()
        sb.append("{")

        val appSettings = settingsDataStore.appSettings.firstOrNull()
        if (appSettings != null) {
            sb.append("\"appSettings\":{")
            sb.append("\"themeMode\":\"${appSettings.themeMode}\",")
            sb.append("\"hideBalance\":${appSettings.hideBalance},")
            sb.append("\"receiptAutoDeleteDays\":${appSettings.receiptAutoDeleteDays},")
            sb.append("\"dashboardPeriod\":\"${appSettings.dashboardPeriod}\"")
            sb.append("},")
        }

        val backupSettings = settingsDataStore.backupSettings.firstOrNull()
        if (backupSettings != null) {
            sb.append("\"backupSettings\":{")
            sb.append("\"backupFolderUri\":${backupSettings.backupFolderUri?.let { "\"$it\"" } ?: "null"},")
            sb.append("\"backupFrequency\":\"${backupSettings.backupFrequency}\",")
            sb.append("\"autoBackupEnabled\":${backupSettings.autoBackupEnabled}")
            sb.append("},")
        }

        val notifSettings = settingsDataStore.notificationSettings.firstOrNull()
        if (notifSettings != null) {
            sb.append("\"notificationSettings\":{")
            sb.append("\"dailyReminderEnabled\":${notifSettings.dailyReminderEnabled},")
            sb.append("\"dailyReminderTime\":${notifSettings.dailyReminderTime?.let { "\"$it\"" } ?: "null"},")
            sb.append("\"weeklyReviewEnabled\":${notifSettings.weeklyReviewEnabled},")
            sb.append("\"monthlyReviewEnabled\":${notifSettings.monthlyReviewEnabled}")
            sb.append("}")
        } else {
            // Remove trailing comma if notifSettings is null
            if (sb.endsWith(",")) sb.deleteCharAt(sb.length - 1)
        }

        sb.append("}")
        return sb.toString()
    }
}
