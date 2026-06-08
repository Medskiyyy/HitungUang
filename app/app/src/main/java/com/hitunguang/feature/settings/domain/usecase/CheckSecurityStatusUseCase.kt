package com.hitunguang.feature.settings.domain.usecase

import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CheckSecurityStatusUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return repository.getSecuritySettings().map { it?.pinHash != null }
    }
}
