package com.hitunguang.feature.account.data.di

import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.account.domain.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AccountDataModule {
    @Binds
    @Singleton
    fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository
}
