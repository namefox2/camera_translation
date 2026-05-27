package com.example.artranslator.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.artranslator.core.database.dao.PhraseDao
import com.example.artranslator.core.database.entity.DownloadedLanguageEntity
import com.example.artranslator.core.database.entity.PhraseEntity

@Database(
    entities = [PhraseEntity::class, DownloadedLanguageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PhrasebookDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao
}
