package com.hitunguang.feature.onboarding.data.di

import com.hitunguang.feature.onboarding.data.repository.UserProfileRepositoryImpl
import com.hitunguang.feature.onboarding.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface OnboardingDataModule {
    @Binds
    @Singleton
    fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository
}
