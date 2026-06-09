package com.hitunguang.feature.category.domain.repository

import com.hitunguang.feature.category.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: String): Flow<List<Category>>
    fun getCategoryById(id: String): Flow<Category?>
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun getTransactionCountForCategory(categoryId: String): Int
    suspend fun deleteCategoryAndSoftDelete(category: Category, now: Long)
    suspend fun restoreDefaultCategories()
    suspend fun getCategoryByIdDirect(id: String): Category?
}
