package com.hitunguang.feature.budget.data.mapper

import com.hitunguang.core.database.entity.BudgetEntity
import com.hitunguang.feature.budget.domain.model.Budget

object BudgetMapper {
    fun toDomain(entity: BudgetEntity): Budget {
        return Budget(
            id = entity.id,
            categoryId = entity.categoryId,
            budgetType = entity.budgetType,
            amount = entity.amount,
            thresholdPercent = entity.thresholdPercent,
            startDate = entity.startDate,
            endDate = entity.endDate,
            isActive = entity.isActive,
            isDeleted = entity.isDeleted,
            deletedAt = entity.deletedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: Budget): BudgetEntity {
        return BudgetEntity(
            id = domain.id,
            categoryId = domain.categoryId,
            budgetType = domain.budgetType,
            amount = domain.amount,
            thresholdPercent = domain.thresholdPercent,
            startDate = domain.startDate,
            endDate = domain.endDate,
            isActive = domain.isActive,
            isDeleted = domain.isDeleted,
            deletedAt = domain.deletedAt,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
