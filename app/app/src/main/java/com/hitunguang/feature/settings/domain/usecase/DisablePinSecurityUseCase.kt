package com.hitunguang.feature.settings.domain.usecase

import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class DisablePinSecurityUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke() {
        val existingSettings = repository.getSecuritySettings().firstOrNull() ?: return
        val now = System.currentTimeMillis()
        val disabledSettings = SecuritySettings(
            id = "security_settings",
            pinHash = null,
            biometricEnabled = false,
            recoveryCodeHash = null,
            createdAt = existingSettings.createdAt,
            updatedAt = now
        )
        repository.saveSecuritySettings(disabledSettings)
    }
}
