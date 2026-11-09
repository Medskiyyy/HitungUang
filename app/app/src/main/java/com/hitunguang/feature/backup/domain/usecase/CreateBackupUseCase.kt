package com.hitunguang.feature.backup.domain.usecase

import com.hitunguang.core.backup.BackupManager
import com.hitunguang.core.datastore.SettingsDataStore
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class CreateBackupUseCase @Inject constructor(
    private val backupManager: BackupManager,
    private val settingsDataStore: SettingsDataStore
) {
    /**
     * Creates a backup to the given [folderUri], or reads it from settings if not provided.
     */
    suspend operator fun invoke(folderUri: String? = null): Result<String> {
        val uri = folderUri
            ?: settingsDataStore.backupSettings.firstOrNull()?.backupFolderUri
            ?: return Result.failure(IllegalStateException("Folder cadangan belum dipilih. Pilih folder dulu di Cadangan & Pemulihan."))

        return backupManager.createBackup(uri)
    }
}
