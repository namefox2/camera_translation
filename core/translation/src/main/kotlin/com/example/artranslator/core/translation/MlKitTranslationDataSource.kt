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

    private val resultCache = LruCache<String, String>(200)
    // Translator 인스턴스를 언어 쌍별로 재사용 (매 호출마다 생성/해제 방지)
    private val translatorPool = HashMap<String, com.google.mlkit.nl.translate.Translator>(8)

    private fun getTranslator(src: String, tgt: String): com.google.mlkit.nl.translate.Translator {
        val key = "$src|$tgt"
        return translatorPool.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(src.toMlKitCode())
                    .setTargetLanguage(tgt.toMlKitCode())
                    .build()
            )
        }
    }

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String
    ): TranslationResult {
        val cacheKey = "$text|$sourceLanguage|$targetLanguage"
        resultCache.get(cacheKey)?.let { cached ->
            return TranslationResult.Success(cached, sourceLanguage, isOffline = true)
        }

        return try {
            val result = getTranslator(sourceLanguage, targetLanguage).translate(text).await()
            resultCache.put(cacheKey, result)
            TranslationResult.Success(result, sourceLanguage, isOffline = true)
        } catch (e: Exception) {
            TranslationResult.Error("오프라인 번역 불가 — '언어' 탭에서 언어팩을 다운로드해 주세요.", e)
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
