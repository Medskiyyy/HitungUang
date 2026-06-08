package com.hitunguang.feature.category.data.repository

import com.hitunguang.core.database.dao.CategoryDao
import com.hitunguang.feature.category.data.mapper.CategoryMapper
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { CategoryMapper.toDomain(it) }
        }
    }

    override fun getCategoriesByType(type: String): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type).map { list ->
            list.map { CategoryMapper.toDomain(it) }
        }
    }

    override fun getCategoryById(id: String): Flow<Category?> {
        return categoryDao.getCategoryById(id).map { entity ->
            entity?.let { CategoryMapper.toDomain(it) }
        }
    }

    override suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(CategoryMapper.toEntity(category))
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(CategoryMapper.toEntity(category))
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(CategoryMapper.toEntity(category))
    }

    override suspend fun getTransactionCountForCategory(categoryId: String): Int {
        return categoryDao.getTransactionCountForCategory(categoryId)
    }

    override suspend fun deleteCategoryAndSoftDelete(category: Category, now: Long) {
        categoryDao.deleteCategoryAndSoftDeleteTransactions(CategoryMapper.toEntity(category), now)
    }
}
