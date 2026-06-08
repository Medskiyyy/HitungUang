package com.hitunguang.feature.transfer.data.di

import com.hitunguang.feature.transfer.data.repository.TransferRepositoryImpl
import com.hitunguang.feature.transfer.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TransferDataModule {
    @Binds
    @Singleton
    fun bindTransferRepository(
        impl: TransferRepositoryImpl
    ): TransferRepository
}
