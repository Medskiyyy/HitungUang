package com.hitunguang.feature.backup.domain.usecase

import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.settings.domain.model.BackupSettings
import javax.inject.Inject

class UpdateBackupSettingsUseCase @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(settings: BackupSettings) {
        settingsDataStore.saveBackupSettings(
            settings.copy(updatedAt = System.currentTimeMillis())
        )
    }
}
