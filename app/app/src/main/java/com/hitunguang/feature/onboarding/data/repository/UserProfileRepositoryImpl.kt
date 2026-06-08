package com.hitunguang.feature.onboarding.data.repository

import com.hitunguang.core.database.dao.UserProfileDao
import com.hitunguang.feature.onboarding.data.mapper.UserProfileMapper
import com.hitunguang.feature.onboarding.domain.model.UserProfile
import com.hitunguang.feature.onboarding.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {
    override fun getUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getUserProfile().map { entity ->
            entity?.let { UserProfileMapper.toDomain(it) }
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdate(UserProfileMapper.toEntity(profile))
    }
}
