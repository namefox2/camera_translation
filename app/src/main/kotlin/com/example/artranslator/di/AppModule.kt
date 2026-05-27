package com.example.artranslator.di

import com.example.artranslator.BuildConfig
import com.example.artranslator.core.translation.TranslationRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Injects the Cloud Translation API key (loaded from local.properties via BuildConfig)
     * into [TranslationRepositoryImpl]. The key is never stored in source code.
     */
    @Provides
    @Singleton
    fun provideApiKey(): String = BuildConfig.TRANSLATION_API_KEY

    @Provides
    @Singleton
    fun configureTranslationRepo(
        repo: TranslationRepositoryImpl,
        apiKey: String
    ): TranslationRepositoryImpl = repo.apply { this.apiKey = apiKey }
}
