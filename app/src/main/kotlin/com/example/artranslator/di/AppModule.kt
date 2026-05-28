package com.example.artranslator.di

import com.example.artranslator.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * DeepL Free API 키를 Named 바인딩으로 제공합니다.
     * 키는 local.properties → BuildConfig를 통해 주입되며, 소스코드에 하드코딩하지 않습니다.
     */
    @Provides
    @Singleton
    @Named("azure_translation_key")
    fun provideAzureTranslationKey(): String = BuildConfig.AZURE_TRANSLATION_KEY
}
