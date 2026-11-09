package com.hitunguang.feature.settings.domain.usecase

import com.hitunguang.core.security.PinHasher
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ValidateRecoveryCodeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(code: String): Boolean {
        val settings = repository.getSecuritySettings().firstOrNull() ?: return false
        val storedHash = settings.recoveryCodeHash ?: return false
        return withContext(Dispatchers.Default) {
            PinHasher.verify(code.trim().uppercase(), storedHash)
        }
    }
}
