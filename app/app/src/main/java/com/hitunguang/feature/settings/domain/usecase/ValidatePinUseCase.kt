package com.hitunguang.feature.settings.domain.usecase

import com.hitunguang.core.security.PinHasher
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ValidatePinUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        val settings = repository.getSecuritySettings().firstOrNull() ?: return false
        val storedHash = settings.pinHash ?: return false
        return PinHasher.hash(pin) == storedHash
    }
}
