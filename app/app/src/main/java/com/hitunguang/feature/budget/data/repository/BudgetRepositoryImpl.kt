package com.hitunguang.feature.budget.data.repository

import com.hitunguang.core.database.dao.BudgetDao
import com.hitunguang.feature.budget.data.mapper.BudgetMapper
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import com.hitunguang.core.database.dao.RecycleBinDao
import com.hitunguang.core.database.entity.RecycleBinEntity
import java.util.UUID

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val recycleBinDao: RecycleBinDao? = null
) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets().map { list ->
            list.map { BudgetMapper.toDomain(it) }
        }
    }

    override fun getActiveBudgets(): Flow<List<Budget>> {
        return budgetDao.getActiveBudgets().map { list ->
            list.map { BudgetMapper.toDomain(it) }
        }
    }

    override suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(BudgetMapper.toEntity(budget))
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(BudgetMapper.toEntity(budget))
    }

    override suspend fun deleteBudget(budget: Budget) {
        val now = System.currentTimeMillis()
        budgetDao.softDeleteBudget(budget.id, now)
        val recycleBinId = UUID.randomUUID().toString()
        val expireAt = now + 30L * 24 * 60 * 60 * 1000L
        val recycleBinEntry = RecycleBinEntity(
            id = recycleBinId,
            entityType = "BUDGET",
            entityId = budget.id,
            deletedAt = now,
            expireAt = expireAt
        )
        recycleBinDao?.addToRecycleBin(recycleBinEntry)
    }
}
