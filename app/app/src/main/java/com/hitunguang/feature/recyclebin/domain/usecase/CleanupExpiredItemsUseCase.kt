package com.hitunguang.feature.recyclebin.domain.usecase

import com.hitunguang.feature.recyclebin.domain.model.CleanupResult
import com.hitunguang.feature.recyclebin.domain.repository.RecycleBinRepository
import javax.inject.Inject

class CleanupExpiredItemsUseCase @Inject constructor(
    private val repository: RecycleBinRepository
) {
    suspend operator fun invoke(currentTime: Long): CleanupResult {
        return repository.cleanupExpiredItems(currentTime)
    }
}
