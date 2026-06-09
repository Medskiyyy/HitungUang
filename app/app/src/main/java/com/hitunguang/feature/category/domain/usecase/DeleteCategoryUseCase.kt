package com.hitunguang.feature.category.domain.usecase

import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category, forceDelete: Boolean = false, now: Long = System.currentTimeMillis()) {
        val transactionCount = categoryRepository.getTransactionCountForCategory(category.id)
        if (transactionCount > 0) {
            if (!forceDelete) {
                throw IllegalStateException("Kategori memiliki transaksi dan memerlukan konfirmasi untuk dihapus.")
            }
            categoryRepository.deleteCategoryAndSoftDelete(category, now)
        } else {
            categoryRepository.deleteCategory(category)
        }
    }
}
