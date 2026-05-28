package com.example.artranslator.core.database.dao

import androidx.room.*
import com.example.artranslator.core.database.entity.DownloadedLanguageEntity
import com.example.artranslator.core.database.entity.PhraseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {

    // ─── Phrases ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM phrases WHERE languageCode = :languageCode ORDER BY category, sortOrder")
    fun getPhrasesForLanguage(languageCode: String): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases WHERE languageCode = :languageCode AND category = :category ORDER BY sortOrder")
    fun getPhrasesByCategory(languageCode: String, category: String): Flow<List<PhraseEntity>>

    @Query("SELECT DISTINCT category FROM phrases WHERE languageCode = :languageCode")
    fun getCategoriesForLanguage(languageCode: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(phrases: List<PhraseEntity>)

    @Delete
    suspend fun deletePhrase(phrase: PhraseEntity)

    @Query("DELETE FROM phrases WHERE id = :id")
    suspend fun deletePhraseById(id: Long)

    @Query("DELETE FROM phrases WHERE languageCode = :languageCode")
    suspend fun deletePhrasesForLanguage(languageCode: String)

    // ─── Downloaded languages ─────────────────────────────────────────────────

    @Query("SELECT * FROM downloaded_languages ORDER BY displayName")
    fun getDownloadedLanguages(): Flow<List<DownloadedLanguageEntity>>

    @Query("SELECT * FROM downloaded_languages WHERE languageCode = :languageCode")
    suspend fun getDownloadedLanguage(languageCode: String): DownloadedLanguageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedLanguage(language: DownloadedLanguageEntity)

    @Query("DELETE FROM downloaded_languages WHERE languageCode = :languageCode")
    suspend fun deleteDownloadedLanguage(languageCode: String)
}
