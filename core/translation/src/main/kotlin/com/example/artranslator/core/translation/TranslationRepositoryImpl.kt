package com.example.artranslator.core.translation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.artranslator.core.database.dao.TranslationCacheDao
import com.example.artranslator.core.database.entity.TranslationCacheEntity
import com.example.artranslator.core.translation.model.DownloadState
import com.example.artranslator.core.translation.model.TranslationResult
import com.google.mlkit.nl.languageid.LanguageIdentification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** 캐시 항목 상한. 초과 시 오래된 100건 삭제 */
private const val CACHE_MAX = 500
private const val CACHE_EVICT = 100

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudDataSource: CloudTranslationDataSource,
    private val mlKitDataSource: MlKitTranslationDataSource,
    private val languagePackManager: LanguagePackManager,
    private val translationCacheDao: TranslationCacheDao,
    @Named("translation_api_key") private val apiKey: String
) : TranslationRepository {

    override suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String?
    ): TranslationResult {
        val src = sourceLanguage ?: identifyLanguage(text) ?: "en"

        return if (isOnline()) {
            // ── 온라인: Cloud Translation이 항상 1순위 ──────────────────────────
            val result = cloudDataSource.translate(text, targetLanguage, src, apiKey)
            if (result is TranslationResult.Success) {
                // Cloud 성공 → DB에 영구 저장 (오프라인 재사용)
                saveToCache(text, src, targetLanguage, result.translatedText)
                result
            } else {
                // Cloud 오류 → DB 캐시 확인 → ML Kit 순으로 폴백
                lookupCacheOrMlKit(text, src, targetLanguage)
            }
        } else {
            // ── 오프라인: 데이터 없을 때만 ─────────────────────────────────────
            // 1순위: DB 캐시 (온라인에서 번역해둔 Cloud 품질 결과)
            // 2순위: ML Kit 온디바이스 (단어 위주, 문장 품질 낮음)
            lookupCacheOrMlKit(text, src, targetLanguage)
        }
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private suspend fun lookupCacheOrMlKit(
        text: String,
        src: String,
        targetLanguage: String
    ): TranslationResult {
        val cached = translationCacheDao.find(text, src, targetLanguage)
        if (cached != null) {
            // DB 캐시 히트 — Cloud 품질을 오프라인에서 재사용
            return TranslationResult.Success(
                translatedText = cached.translatedText,
                sourceLanguage = src,
                isOffline = true    // 오프라인 재사용임을 UI에 표시
            )
        }
        // ML Kit 폴백 (단어는 되지만 문장 품질 낮음)
        return mlKitDataSource.translate(text, targetLanguage, src)
    }

    private suspend fun saveToCache(
        text: String,
        src: String,
        targetLanguage: String,
        translated: String
    ) {
        // 상한 초과 시 오래된 순 100건 삭제
        if (translationCacheDao.count() >= CACHE_MAX) {
            translationCacheDao.deleteOldest(CACHE_EVICT)
        }
        translationCacheDao.upsert(
            TranslationCacheEntity(
                sourceText = text,
                sourceLanguage = src,
                targetLanguage = targetLanguage,
                translatedText = translated
            )
        )
    }

    // ─── 나머지 구현 ─────────────────────────────────────────────────────────

    override suspend fun identifyLanguage(text: String): String? {
        return try {
            val languageId = LanguageIdentification.getClient()
            val langCode = languageId.identifyLanguage(text).await()
            languageId.close()
            if (langCode == "und") null else langCode
        } catch (e: Exception) {
            null
        }
    }

    override fun downloadLanguageModel(languageCode: String, requireWifi: Boolean): Flow<DownloadState> =
        languagePackManager.downloadModel(languageCode, requireWifi)

    override suspend fun deleteLanguageModel(languageCode: String): Boolean =
        languagePackManager.deleteModel(languageCode)

    override suspend fun isModelDownloaded(languageCode: String): Boolean =
        languagePackManager.isModelAvailable(languageCode)

    override suspend fun getDownloadedLanguages(): List<String> = emptyList()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
