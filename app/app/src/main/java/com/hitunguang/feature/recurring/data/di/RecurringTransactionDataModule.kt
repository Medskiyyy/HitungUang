package com.hitunguang.feature.recurring.data.di

import com.hitunguang.feature.recurring.data.repository.RecurringTransactionRepositoryImpl
import com.hitunguang.feature.recurring.domain.repository.RecurringTransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RecurringTransactionDataModule {
    @Binds
    @Singleton
    fun bindRecurringTransactionRepository(
        impl: RecurringTransactionRepositoryImpl
    ): RecurringTransactionRepository
}
