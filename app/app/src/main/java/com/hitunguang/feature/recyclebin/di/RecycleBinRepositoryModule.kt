package com.hitunguang.feature.recyclebin.di

import com.hitunguang.feature.recyclebin.data.repository.RecycleBinRepositoryImpl
import com.hitunguang.feature.recyclebin.domain.repository.RecycleBinRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecycleBinRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecycleBinRepository(
        recycleBinRepositoryImpl: RecycleBinRepositoryImpl
    ): RecycleBinRepository
}
