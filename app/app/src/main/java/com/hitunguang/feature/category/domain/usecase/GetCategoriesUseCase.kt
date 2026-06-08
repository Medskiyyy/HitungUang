package com.hitunguang.feature.category.domain.usecase

import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(type: String? = null): Flow<List<Category>> {
        return if (type != null) {
            categoryRepository.getCategoriesByType(type)
        } else {
            categoryRepository.getAllCategories()
        }
    }
}
