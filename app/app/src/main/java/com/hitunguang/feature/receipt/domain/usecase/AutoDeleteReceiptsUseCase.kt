package com.hitunguang.feature.receipt.domain.usecase

import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.receipt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class AutoDeleteReceiptsUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke() {
        val appSettings = settingsDataStore.appSettings.firstOrNull() ?: return
        val deleteDays = appSettings.receiptAutoDeleteDays
        
        // 0 or negative means "Never"
        if (deleteDays <= 0) return
        
        val thresholdMillis = System.currentTimeMillis() - deleteDays.toLong() * 24 * 60 * 60 * 1000L
        
        val allReceipts = receiptRepository.getAllReceipts().firstOrNull() ?: return
        for (receipt in allReceipts) {
            val dateToCheck = receipt.receiptDate ?: receipt.createdAt
            if (dateToCheck < thresholdMillis) {
                // Auto delete receipt photo and DB entry to save space
                receiptRepository.deleteReceipt(receipt)
            }
        }
    }
}
