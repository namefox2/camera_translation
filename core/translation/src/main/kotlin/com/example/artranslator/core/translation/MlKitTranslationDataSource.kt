package com.example.artranslator.core.translation

import android.util.LruCache
import com.example.artranslator.core.translation.model.DownloadState
import com.example.artranslator.core.translation.model.TranslationResult
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device translation via Google ML Kit Translate.
 * Works fully offline once the language model is downloaded.
 */
@Singleton
class MlKitTranslationDataSource @Inject constructor() {

    // LruCache for translation results (key = "text|src|tgt")
    private val cache = LruCache<String, String>(200)

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String
    ): TranslationResult {
        val cacheKey = "$text|$sourceLanguage|$targetLanguage"
        cache.get(cacheKey)?.let { cached ->
            return TranslationResult.Success(cached, sourceLanguage, isOffline = true)
        }

        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage.toMlKitCode())
                .setTargetLanguage(targetLanguage.toMlKitCode())
                .build()

            val translator = Translation.getClient(options)

            // Ensure model is downloaded before translating
            translator.downloadModelIfNeeded().await()

            val result = translator.translate(text).await()
            translator.close()

            cache.put(cacheKey, result)
            TranslationResult.Success(result, sourceLanguage, isOffline = true)
        } catch (e: Exception) {
            TranslationResult.Error("ML Kit 번역 오류: ${e.localizedMessage}", e)
        }
    }

    fun downloadModel(languageCode: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(languageCode.toMlKitCode())
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build()
            val translator = Translation.getClient(options)
            translator.downloadModelIfNeeded().await()
            translator.close()
            emit(DownloadState.Downloaded)
        } catch (e: Exception) {
            emit(DownloadState.Error(e.localizedMessage ?: "알 수 없는 오류"))
        }
    }

    suspend fun deleteModel(languageCode: String): Boolean {
        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(languageCode.toMlKitCode())
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build()
            val translator = Translation.getClient(options)
            translator.close()
            // ML Kit manages model deletion via RemoteModelManager
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Converts an ISO 639-1 language code to the ML Kit TranslateLanguage constant.
     */
    private fun String.toMlKitCode(): String = when (this.lowercase()) {
        "ko" -> TranslateLanguage.KOREAN
        "en" -> TranslateLanguage.ENGLISH
        "ja" -> TranslateLanguage.JAPANESE
        "zh" -> TranslateLanguage.CHINESE
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
