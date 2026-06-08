package com.hitunguang.feature.transaction.data.di

import com.hitunguang.feature.transaction.data.repository.AttachmentRepositoryImpl
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.repository.AttachmentRepository
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TransactionDataModule {
    @Binds
    @Singleton
    fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    fun bindAttachmentRepository(
        impl: AttachmentRepositoryImpl
    ): AttachmentRepository
}
