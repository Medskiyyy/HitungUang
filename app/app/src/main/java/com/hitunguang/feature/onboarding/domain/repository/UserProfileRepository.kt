package com.hitunguang.feature.onboarding.domain.repository

import com.hitunguang.feature.onboarding.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
}
