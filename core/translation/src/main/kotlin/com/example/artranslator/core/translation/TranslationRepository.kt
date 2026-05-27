package com.example.artranslator.core.translation

import com.example.artranslator.core.translation.model.DownloadState
import com.example.artranslator.core.translation.model.TranslationResult
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction layer over online (Cloud) and offline (ML Kit) translation.
 * Implementations decide which source to use based on network availability
 * and whether the required offline model is available.
 */
interface TranslationRepository {

    /**
     * Translate [text] from [sourceLanguage] (null = auto-detect) to [targetLanguage].
     * Falls back to offline translation when no network is available.
     */
    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): TranslationResult

    /**
     * Identify the language of the given [text].
     * Returns the ISO 639-1 language code or null if confidence is too low.
     */
    suspend fun identifyLanguage(text: String): String?

    /**
     * Download the offline ML Kit model for [languageCode].
     * [requireWifi] = true 이면 WiFi 연결 시에만 다운로드합니다.
     * Emits [DownloadState] progress updates.
     */
    fun downloadLanguageModel(languageCode: String, requireWifi: Boolean = false): Flow<DownloadState>

    /**
     * Delete the offline ML Kit model for [languageCode].
     */
    suspend fun deleteLanguageModel(languageCode: String): Boolean

    /**
     * Returns true if the offline model for [languageCode] is available.
     */
    suspend fun isModelDownloaded(languageCode: String): Boolean

    /**
     * Returns a list of language codes whose offline models are downloaded.
     */
    suspend fun getDownloadedLanguages(): List<String>
}
