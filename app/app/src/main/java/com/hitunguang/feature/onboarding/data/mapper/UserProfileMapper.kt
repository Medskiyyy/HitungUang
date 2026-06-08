package com.hitunguang.feature.onboarding.data.mapper

import com.hitunguang.core.database.entity.UserProfileEntity
import com.hitunguang.feature.onboarding.domain.model.UserProfile

object UserProfileMapper {
    fun toDomain(entity: UserProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            name = entity.name,
            occupation = entity.occupation,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: UserProfile): UserProfileEntity {
        return UserProfileEntity(
            id = domain.id,
            name = domain.name,
            occupation = domain.occupation,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
