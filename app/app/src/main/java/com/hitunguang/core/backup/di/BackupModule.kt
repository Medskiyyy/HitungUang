package com.hitunguang.core.backup.di

import android.content.Context
import com.hitunguang.core.backup.BackupManager
import com.hitunguang.core.backup.RestoreManager
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        settingsDataStore: SettingsDataStore,
        database: HitungUangDatabase
    ): BackupManager = BackupManager(context, settingsDataStore, database)

    @Provides
    @Singleton
    fun provideRestoreManager(
        @ApplicationContext context: Context,
        settingsDataStore: SettingsDataStore,
        database: HitungUangDatabase
    ): RestoreManager = RestoreManager(context, settingsDataStore, database)
}
