package com.hitunguang.feature.settings.domain.usecase

import com.hitunguang.core.security.PinHasher
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject

class SavePinUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(pin: String): String {
        val existingSettings = repository.getSecuritySettings().firstOrNull()
        
        val rawRecoveryCode = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        val recoveryHash = PinHasher.hash(rawRecoveryCode)
        
        val now = System.currentTimeMillis()
        val newSettings = SecuritySettings(
            id = "security_settings",
            pinHash = PinHasher.hash(pin),
            biometricEnabled = existingSettings?.biometricEnabled ?: false,
            recoveryCodeHash = recoveryHash,
            createdAt = existingSettings?.createdAt ?: now,
            updatedAt = now
        )
        
        repository.saveSecuritySettings(newSettings)
        return rawRecoveryCode
    }
}
