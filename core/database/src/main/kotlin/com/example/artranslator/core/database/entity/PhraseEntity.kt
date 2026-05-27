package com.example.artranslator.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single travel phrase stored in the Room database.
 *
 * @param id          Auto-generated primary key
 * @param languageCode ISO 639-1 language code (e.g. "ja", "fr")
 * @param category    Phrase category (e.g. "greeting", "restaurant")
 * @param originalText Korean text (source)
 * @param translatedText Translated text in [languageCode]
 * @param pronunciation Romanisation / phonetic pronunciation
 * @param sortOrder   Display order within the category
 */
@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val languageCode: String,
    val category: String,
    val originalText: String,
    val translatedText: String,
    val pronunciation: String = "",
    val sortOrder: Int = 0
)

/**
 * Downloaded language model metadata.
 */
@Entity(tableName = "downloaded_languages")
data class DownloadedLanguageEntity(
    @PrimaryKey val languageCode: String,
    val displayName: String,
    val nativeName: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val modelSizeMb: Int = 0
)
