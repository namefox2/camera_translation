package com.example.artranslator.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.artranslator.core.database.PhrasebookDatabase
import com.example.artranslator.core.database.dao.PhraseDao
import com.example.artranslator.core.database.dao.TranslationCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePhrasebookDatabase(
        @ApplicationContext context: Context
    ): PhrasebookDatabase = Room.databaseBuilder(
        context,
        PhrasebookDatabase::class.java,
        "phrasebook.db"
    )
        .addMigrations(PhrasebookDatabase.MIGRATION_1_2)
        .build()

    @Provides
    @Singleton
    fun providePhraseDao(database: PhrasebookDatabase): PhraseDao =
        database.phraseDao()

    @Provides
    @Singleton
    fun provideTranslationCacheDao(database: PhrasebookDatabase): TranslationCacheDao =
        database.translationCacheDao()
}
