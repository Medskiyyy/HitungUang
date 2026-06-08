package com.hitunguang.feature.backup.domain.usecase

import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.settings.domain.model.BackupSettings
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class GetBackupSettingsUseCase @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(): BackupSettings? {
        return settingsDataStore.backupSettings.firstOrNull()
    }
}
