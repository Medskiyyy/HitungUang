package com.hitunguang.feature.category.domain.usecase

import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import javax.inject.Inject

class SaveCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category, isEdit: Boolean) {
        if (category.name.trim().isEmpty()) {
            throw IllegalArgumentException("Nama kategori tidak boleh kosong.")
        }
        
        if (isEdit) {
            val existing = categoryRepository.getCategoryByIdDirect(category.id)
            if (existing != null && existing.isDefault) {
                if (existing.name != category.name || existing.categoryType != category.categoryType) {
                    throw IllegalStateException("Kategori default tidak dapat diubah namanya atau tipenya.")
                }
            }
            categoryRepository.updateCategory(category)
        } else {
            categoryRepository.insertCategory(category)
        }
    }
}
