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

private const val CACHE_MAX = 500
private const val CACHE_EVICT = 100

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val azureDataSource: AzureTranslationDataSource,
    private val mlKitDataSource: MlKitTranslationDataSource,
    private val languagePackManager: LanguagePackManager,
    private val translationCacheDao: TranslationCacheDao,
    @Named("azure_translation_key") private val apiKey: String
) : TranslationRepository {

    override suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String?
    ): TranslationResult {
        val src = sourceLanguage ?: identifyLanguage(text) ?: "en"

        return if (isOnline()) {
            val result = azureDataSource.translate(text, targetLanguage, src, apiKey)
            if (result is TranslationResult.Success) {
                saveToCache(text, src, targetLanguage, result.translatedText)
                result
            } else {
                // DB 캐시 확인
                val cached = translationCacheDao.find(text, src, targetLanguage)
                if (cached != null) {
                    TranslationResult.Success(cached.translatedText, src, isOffline = false)
                } else {
                    // Azure 실패 시 ML Kit 오프라인으로 폴백 (키 오류·한도 초과 등 포함)
                    val mlResult = mlKitDataSource.translate(text, targetLanguage, src)
                    if (mlResult is TranslationResult.Success) mlResult else result
                }
            }
        } else {
            lookupCacheOrMlKit(text, src, targetLanguage)
        }
    }

    private suspend fun lookupCacheOrMlKit(text: String, src: String, targetLanguage: String): TranslationResult {
        val cached = translationCacheDao.find(text, src, targetLanguage)
        if (cached != null) {
            return TranslationResult.Success(cached.translatedText, src, isOffline = false)
        }
        return mlKitDataSource.translate(text, targetLanguage, src)
    }

    private suspend fun saveToCache(text: String, src: String, targetLanguage: String, translated: String) {
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

    private val languageIdClient by lazy { LanguageIdentification.getClient() }

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
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
