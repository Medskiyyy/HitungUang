package com.hitunguang.core.ocr.di

import com.hitunguang.core.ocr.OcrManager
import com.hitunguang.core.ocr.OcrManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {

    @Binds
    @Singleton
    abstract fun bindOcrManager(
        ocrManagerImpl: OcrManagerImpl
    ): OcrManager
}
