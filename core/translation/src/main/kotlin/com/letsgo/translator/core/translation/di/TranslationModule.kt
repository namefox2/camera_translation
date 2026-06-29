package com.letsgo.translator.core.translation.di

import com.letsgo.translator.core.translation.TranslationRepository
import com.letsgo.translator.core.translation.TranslationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranslationModule {

    @Binds
    @Singleton
    abstract fun bindTranslationRepository(
        impl: TranslationRepositoryImpl
    ): TranslationRepository
}
