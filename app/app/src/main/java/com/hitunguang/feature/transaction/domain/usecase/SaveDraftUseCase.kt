package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.transaction.domain.model.TransactionDraft
import javax.inject.Inject

class SaveDraftUseCase @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(draft: TransactionDraft) {
        settingsDataStore.saveTransactionDraft(draft)
    }
}
