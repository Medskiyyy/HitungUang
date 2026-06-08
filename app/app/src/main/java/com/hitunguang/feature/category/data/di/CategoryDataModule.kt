package com.hitunguang.feature.category.data.di

import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CategoryDataModule {
    @Binds
    @Singleton
    fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository
}
