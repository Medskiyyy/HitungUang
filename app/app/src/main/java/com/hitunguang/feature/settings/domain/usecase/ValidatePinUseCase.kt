package com.hitunguang.feature.settings.domain.usecase

import com.hitunguang.core.security.PinHasher
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ValidatePinUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        val settings = repository.getSecuritySettings().firstOrNull() ?: return false
        val storedHash = settings.pinHash ?: return false
        val isValid = withContext(Dispatchers.Default) { PinHasher.verify(pin, storedHash) }
        if (isValid && (PinHasher.isLegacy(storedHash) || settings.pinLength != pin.length)) {
            val upgradedHash = withContext(Dispatchers.Default) { PinHasher.hash(pin) }
            repository.saveSecuritySettings(
                settings.copy(
                    pinHash = upgradedHash,
                    pinLength = pin.length,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return isValid
    }
}
