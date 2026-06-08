package com.hitunguang.feature.settings.domain.usecase

import com.hitunguang.core.security.PinHasher
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ValidateRecoveryCodeUseCase @Inject constructor(
    private val repository: SettingsRepository,
    private val disablePinSecurityUseCase: DisablePinSecurityUseCase
) {
    suspend operator fun invoke(code: String): Boolean {
        val settings = repository.getSecuritySettings().firstOrNull() ?: return false
        val storedHash = settings.recoveryCodeHash ?: return false
        val isValid = PinHasher.hash(code.trim().uppercase()) == storedHash
        if (isValid) {
            disablePinSecurityUseCase()
        }
        return isValid
    }
}
