package com.example.artranslator.core.translation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.artranslator.core.translation.model.DownloadState
import com.example.artranslator.core.translation.model.TranslationResult
import com.google.mlkit.nl.languageid.LanguageIdentification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudDataSource: CloudTranslationDataSource,
    private val mlKitDataSource: MlKitTranslationDataSource,
    private val languagePackManager: LanguagePackManager,
    // API 키는 AppModule의 @Named("translation_api_key")로 제공됩니다.
    // local.properties → BuildConfig 경유, 소스코드 하드코딩 금지.
    @Named("translation_api_key") private val apiKey: String
) : TranslationRepository {

    override suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String?
    ): TranslationResult {
        val src = sourceLanguage ?: identifyLanguage(text) ?: "en"

        return if (isOnline()) {
            // 온라인: Cloud Translation API (고품질)
            val result = cloudDataSource.translate(text, targetLanguage, src, apiKey)
            if (result is TranslationResult.Error) {
                // Cloud API 오류 시 오프라인 ML Kit으로 폴백
                mlKitDataSource.translate(text, targetLanguage, src)
            } else {
                result
            }
        } else {
            // 오프라인: ML Kit 온디바이스 번역
            mlKitDataSource.translate(text, targetLanguage, src)
        }
    }

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

    override fun downloadLanguageModel(languageCode: String): Flow<DownloadState> =
        languagePackManager.downloadModel(languageCode)

    override suspend fun deleteLanguageModel(languageCode: String): Boolean =
        languagePackManager.deleteModel(languageCode)

    override suspend fun isModelDownloaded(languageCode: String): Boolean =
        languagePackManager.isModelAvailable(languageCode)

    override suspend fun getDownloadedLanguages(): List<String> = emptyList()

    // ─── 네트워크 상태 확인 ────────────────────────────────────────────────────

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
