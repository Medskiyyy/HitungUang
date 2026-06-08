package com.hitunguang.core.balance.di

import com.hitunguang.core.balance.BalanceValidator
import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.core.database.dao.TransactionDao
import com.hitunguang.core.database.dao.TransferDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BalanceModule {

    @Provides
    fun provideBalanceValidator(
        accountDao: AccountDao,
        transactionDao: TransactionDao,
        transferDao: TransferDao
    ): BalanceValidator {
        return BalanceValidator(accountDao, transactionDao, transferDao)
    }
}
