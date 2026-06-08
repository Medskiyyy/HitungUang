package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.core.datastore.SettingsDataStore
import javax.inject.Inject

class ClearDraftUseCase @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke() {
        settingsDataStore.clearTransactionDraft()
    }
}
