package com.hitunguang.feature.category.data.repository

import com.hitunguang.core.database.dao.CategoryDao
import com.hitunguang.feature.category.data.mapper.CategoryMapper
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.RecycleBinEntity
import java.util.UUID

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
        val now = System.currentTimeMillis()
        categoryDao.softDeleteCategory(category.id, now)
        val recycleBinId = UUID.randomUUID().toString()
        val expireAt = now + 30L * 24 * 60 * 60 * 1000L
        val recycleBinEntry = RecycleBinEntity(
            id = recycleBinId,
            entityType = "CATEGORY",
            entityId = category.id,
            deletedAt = now,
            expireAt = expireAt
        )
        categoryDao.insertRecycleBinEntry(recycleBinEntry)
    }

    override suspend fun getTransactionCountForCategory(categoryId: String): Int {
        return categoryDao.getTransactionCountForCategory(categoryId)
    }

    override suspend fun deleteCategoryAndSoftDelete(category: Category, now: Long) {
        categoryDao.deleteCategoryAndSoftDeleteTransactions(CategoryMapper.toEntity(category), now)
    }

    override suspend fun restoreDefaultCategories() {
        val now = System.currentTimeMillis()
        val defaultCategories = listOf(
            CategoryEntity("default_expense_makanan", "Makanan", "EXPENSE", "restaurant", true, false, now, now, false, null),
            CategoryEntity("default_expense_transportasi", "Transportasi", "EXPENSE", "directions_car", true, false, now, now, false, null),
            CategoryEntity("default_expense_belanja", "Belanja", "EXPENSE", "shopping_cart", true, false, now, now, false, null),
            CategoryEntity("default_expense_hiburan", "Hiburan", "EXPENSE", "movie", true, false, now, now, false, null),
            CategoryEntity("default_expense_tagihan", "Tagihan", "EXPENSE", "receipt", true, false, now, now, false, null),
            CategoryEntity("default_expense_lain_lain", "Lain-lain", "EXPENSE", "category", true, false, now, now, false, null),
            
            CategoryEntity("default_income_gaji", "Gaji", "INCOME", "payments", true, false, now, now, false, null),
            CategoryEntity("default_income_investasi", "Investasi", "INCOME", "trending_up", true, false, now, now, false, null),
            CategoryEntity("default_income_bonus", "Bonus", "INCOME", "redeem", true, false, now, now, false, null),
            CategoryEntity("default_income_lain_lain", "Lain-lain", "INCOME", "category", true, false, now, now, false, null)
        )
        categoryDao.restoreDefaultCategories(defaultCategories, now)
    }

    override suspend fun getCategoryByIdDirect(id: String): Category? {
        return categoryDao.getCategoryByIdDirect(id)?.let { CategoryMapper.toDomain(it) }
    }
}
