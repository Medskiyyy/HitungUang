package com.hitunguang.feature.budget.data.di

import com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface BudgetDataModule {
    @Binds
    @Singleton
    fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository
}
