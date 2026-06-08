package com.hitunguang.feature.recyclebin.domain.usecase

import com.hitunguang.feature.recyclebin.domain.repository.RecycleBinRepository
import javax.inject.Inject

class CleanupExpiredItemsUseCase @Inject constructor(
    private val repository: RecycleBinRepository
) {
    suspend operator fun invoke(currentTime: Long) {
        repository.cleanupExpiredItems(currentTime)
    }
}
