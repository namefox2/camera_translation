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
            // ── 온라인: Cloud Translation만 사용 (ML Kit 폴백 없음) ─────────────
            val result = cloudDataSource.translate(text, targetLanguage, src, apiKey)
            if (result is TranslationResult.Success) {
                saveToCache(text, src, targetLanguage, result.translatedText)
                result
            } else {
                // Cloud 실패 → DB 캐시만 확인 (온라인에서는 ML Kit 자동 사용 안 함)
                val cached = translationCacheDao.find(text, src, targetLanguage)
                if (cached != null) {
                    TranslationResult.Success(cached.translatedText, src, isOffline = false)
                } else {
                    result  // Cloud 에러 그대로 반환 (침묵 폴백 없음)
                }
            }
        } else {
            // ── 오프라인: DB 캐시 → ML Kit (팩 다운로드된 경우만) ───────────────
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
            // DB 캐시 히트 — Cloud 번역 결과를 저장해 둔 것이므로 isOffline=false
            return TranslationResult.Success(
                translatedText = cached.translatedText,
                sourceLanguage = src,
                isOffline = false
            )
        }
        // ML Kit 폴백: 모델 없으면 에러 반환 (isOffline=true)
        return mlKitDataSource.translate(text, targetLanguage, src)
    }

    private suspend fun saveToCache(
        text: String,
        src: String,
        targetLanguage: String,
        translated: String
    ) {
        // count → evict → upsert 단일 트랜잭션 (TOCTOU 방지)
        translationCacheDao.upsertWithEviction(
            entity = TranslationCacheEntity(
                sourceText = text,
                sourceLanguage = src,
                targetLanguage = targetLanguage,
                translatedText = translated
            ),
            maxSize = CACHE_MAX,
            evictCount = CACHE_EVICT
        )
    }

    // LanguageIdentification 클라이언트를 한 번만 생성해 재사용 (호출마다 할당 방지)
    private val languageIdClient by lazy { LanguageIdentification.getClient() }

    // ─── 나머지 구현 ─────────────────────────────────────────────────────────

    override suspend fun identifyLanguage(text: String): String? {
        return try {
            val langCode = languageIdClient.identifyLanguage(text).await()
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
        // INTERNET: 네트워크가 인터넷 접근을 선언함
        // VALIDATED: 실제로 인터넷 연결이 확인됨 (캡티브 포털 등 false positive 방지)
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
