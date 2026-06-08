package com.hitunguang.feature.transaction.domain.usecase

import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.transaction.domain.model.TransactionDraft
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDraftUseCase @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    operator fun invoke(): Flow<TransactionDraft?> {
        return settingsDataStore.transactionDraft
    }
}
