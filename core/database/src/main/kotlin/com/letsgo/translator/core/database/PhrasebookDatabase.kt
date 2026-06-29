package com.letsgo.translator.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.letsgo.translator.core.database.dao.PhraseDao
import com.letsgo.translator.core.database.dao.TranslationCacheDao
import com.letsgo.translator.core.database.entity.DownloadedLanguageEntity
import com.letsgo.translator.core.database.entity.PhraseEntity
import com.letsgo.translator.core.database.entity.TranslationCacheEntity

@Database(
    entities = [
        PhraseEntity::class,
        DownloadedLanguageEntity::class,
        TranslationCacheEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PhrasebookDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao
    abstract fun translationCacheDao(): TranslationCacheDao

    companion object {
        /** v1 → v2: translation_cache 테이블 추가 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS translation_cache (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sourceText TEXT NOT NULL,
                        sourceLanguage TEXT NOT NULL,
                        targetLanguage TEXT NOT NULL,
                        translatedText TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_translation_cache_sourceText_sourceLanguage_targetLanguage
                    ON translation_cache (sourceText, sourceLanguage, targetLanguage)
                """.trimIndent())
            }
        }
    }
}
