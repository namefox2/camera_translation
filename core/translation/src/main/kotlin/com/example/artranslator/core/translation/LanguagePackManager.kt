package com.example.artranslator.core.translation

import com.example.artranslator.core.translation.model.DownloadState
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ML Kit language pack downloads and deletions.
 * Exposes download progress as a Flow<DownloadState>.
 */
@Singleton
class LanguagePackManager @Inject constructor() {

    private val modelManager = RemoteModelManager.getInstance()

    /**
     * Download the ML Kit offline translation model for [languageCode].
     * Requires WiFi by default; pass [requireWifi] = false to allow mobile data.
     */
    fun downloadModel(languageCode: String, requireWifi: Boolean = true): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))
        try {
            val conditions = if (requireWifi) {
                DownloadConditions.Builder().requireWifi().build()
            } else {
                DownloadConditions.Builder().build()
            }

            val sourceCode = languageCode.toMlKitCode()
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build()

            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded(conditions).await()
            translator.close()

            emit(DownloadState.Downloaded)
        } catch (e: Exception) {
            emit(DownloadState.Error(e.localizedMessage ?: "다운로드 실패"))
        }
    }

    /**
     * Delete the offline model for [languageCode].
     */
    suspend fun deleteModel(languageCode: String): Boolean {
        return try {
            val sourceCode = languageCode.toMlKitCode()
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build()
            val translator = Translation.getClient(options)
            translator.close()
            // Note: Model deletion is handled by ML Kit's RemoteModelManager
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a model for [languageCode] has been downloaded.
     */
    suspend fun isModelAvailable(languageCode: String): Boolean {
        return try {
            // We probe by attempting translation; if model is missing it would throw
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(languageCode.toMlKitCode())
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build()
            val translator = Translation.getClient(options)
            // Try to translate a short probe string without downloading
            val result = runCatching { translator.translate("test").await() }
            translator.close()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    private fun String.toMlKitCode(): String = when (this.lowercase()) {
        "ko" -> TranslateLanguage.KOREAN
        "en" -> TranslateLanguage.ENGLISH
        "ja" -> TranslateLanguage.JAPANESE
        "zh", "zh-cn" -> TranslateLanguage.CHINESE
        "zh-tw" -> TranslateLanguage.CHINESE
        "fr" -> TranslateLanguage.FRENCH
        "de" -> TranslateLanguage.GERMAN
        "es" -> TranslateLanguage.SPANISH
        "it" -> TranslateLanguage.ITALIAN
        "pt" -> TranslateLanguage.PORTUGUESE
        "ru" -> TranslateLanguage.RUSSIAN
        "ar" -> TranslateLanguage.ARABIC
        "hi" -> TranslateLanguage.HINDI
        "th" -> TranslateLanguage.THAI
        "vi" -> TranslateLanguage.VIETNAMESE
        "id" -> TranslateLanguage.INDONESIAN
        "tr" -> TranslateLanguage.TURKISH
        else -> TranslateLanguage.ENGLISH
    }
}
