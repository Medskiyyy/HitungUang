package com.hitunguang.feature.category.data.mapper

import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.feature.category.domain.model.Category

object CategoryMapper {
    fun toDomain(entity: CategoryEntity): Category {
        return Category(
            id = entity.id,
            name = entity.name,
            categoryType = entity.categoryType,
            icon = entity.icon,
            isDefault = entity.isDefault,
            isPinned = entity.isPinned,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: Category): CategoryEntity {
        return CategoryEntity(
            id = domain.id,
            name = domain.name,
            categoryType = domain.categoryType,
            icon = domain.icon,
            isDefault = domain.isDefault,
            isPinned = domain.isPinned,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
