package com.hitunguang.feature.recyclebin.domain.usecase

import com.hitunguang.feature.recyclebin.domain.model.RecycleBinItem
import com.hitunguang.feature.recyclebin.domain.repository.RecycleBinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDeletedItemsUseCase @Inject constructor(
    private val repository: RecycleBinRepository
) {
    operator fun invoke(): Flow<List<RecycleBinItem>> {
        return repository.getDeletedItems()
    }
}
