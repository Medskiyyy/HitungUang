package com.hitunguang.feature.settings.domain.usecase

import com.hitunguang.core.security.PinHasher
import com.hitunguang.feature.settings.domain.model.PinLockState
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class SavePinUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(pin: String): String {
        val existingSettings = repository.getSecuritySettings().firstOrNull()
        
        val rawRecoveryCode = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        val (pinHash, recoveryHash) = withContext(Dispatchers.Default) {
            PinHasher.hash(pin) to PinHasher.hash(rawRecoveryCode)
        }
        
        val now = System.currentTimeMillis()
        val newSettings = SecuritySettings(
            id = "security_settings",
            pinHash = pinHash,
            biometricEnabled = existingSettings?.biometricEnabled ?: false,
            recoveryCodeHash = recoveryHash,
            createdAt = existingSettings?.createdAt ?: now,
            updatedAt = now,
            pinLength = pin.length
        )
        
        repository.saveSecuritySettings(newSettings)
        repository.savePinLockState(PinLockState())
        return rawRecoveryCode
    }
}
