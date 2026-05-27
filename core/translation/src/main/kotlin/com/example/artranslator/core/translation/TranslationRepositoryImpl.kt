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
import javax.inject.Singleton

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudDataSource: CloudTranslationDataSource,
    private val mlKitDataSource: MlKitTranslationDataSource,
    private val languagePackManager: LanguagePackManager
) : TranslationRepository {

    // API key is injected via the :app BuildConfig via DI in AppModule
    var apiKey: String = ""

    override suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String?
    ): TranslationResult {
        val src = sourceLanguage ?: identifyLanguage(text) ?: "en"

        return if (isOnline()) {
            // Prefer Cloud Translation for higher quality
            val result = cloudDataSource.translate(text, targetLanguage, src, apiKey)
            if (result is TranslationResult.Error) {
                // Fallback to offline on API error
                mlKitDataSource.translate(text, targetLanguage, src)
            } else {
                result
            }
        } else {
            // Offline fallback
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

    override suspend fun getDownloadedLanguages(): List<String> {
        // In a full implementation this would query the ML Kit RemoteModelManager
        return emptyList()
    }

    // ─── Connectivity ─────────────────────────────────────────────────────────

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
