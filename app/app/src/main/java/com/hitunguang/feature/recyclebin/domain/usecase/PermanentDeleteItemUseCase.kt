package com.hitunguang.feature.recyclebin.domain.usecase

import com.hitunguang.feature.recyclebin.domain.repository.RecycleBinRepository
import javax.inject.Inject

class PermanentDeleteItemUseCase @Inject constructor(
    private val repository: RecycleBinRepository
) {
    suspend operator fun invoke(entityId: String, entityType: String) {
        repository.permanentDeleteItem(entityId, entityType)
    }
}
