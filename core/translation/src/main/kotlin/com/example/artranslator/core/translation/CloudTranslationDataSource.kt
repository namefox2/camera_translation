package com.example.artranslator.core.translation

import android.util.LruCache
import com.example.artranslator.core.translation.model.TranslationResult
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

// ─── Retrofit interface ───────────────────────────────────────────────────────

private interface CloudTranslationApi {
    @POST("v2/translate")
    suspend fun translate(
        @Query("key") apiKey: String,
        @Body request: TranslateRequest
    ): TranslateResponse
}

data class TranslateRequest(
    @SerializedName("q") val q: List<String>,
    @SerializedName("target") val target: String,
    @SerializedName("source") val source: String? = null,
    @SerializedName("format") val format: String = "text"
)

data class TranslateResponse(
    @SerializedName("data") val data: TranslateData?
)

data class TranslateData(
    @SerializedName("translations") val translations: List<Translation>?
)

data class Translation(
    @SerializedName("translatedText") val translatedText: String,
    @SerializedName("detectedSourceLanguage") val detectedSourceLanguage: String?
)

// ─── Data source ─────────────────────────────────────────────────────────────

/**
 * Calls the Google Cloud Translation API v2.
 * Results are cached with an LruCache to avoid duplicate API calls.
 */
@Singleton
class CloudTranslationDataSource @Inject constructor() {

    private val api: CloudTranslationApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            .baseUrl("https://translation.googleapis.com/language/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudTranslationApi::class.java)
    }

    // LruCache: key = "$sourceText|$target", max 200 entries
    private val cache = LruCache<String, String>(200)

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String?,
        apiKey: String
    ): TranslationResult {
        val cacheKey = "$text|$targetLanguage|${sourceLanguage ?: "auto"}"
        cache.get(cacheKey)?.let { cached ->
            return TranslationResult.Success(
                translatedText = cached,
                sourceLanguage = sourceLanguage,
                isOffline = false
            )
        }

        return try {
            val response = api.translate(
                apiKey = apiKey,
                request = TranslateRequest(
                    q = listOf(text),
                    target = targetLanguage,
                    source = sourceLanguage
                )
            )
            val translation = response.data?.translations?.firstOrNull()
                ?: return TranslationResult.Error("번역 결과가 비어 있습니다.")

            cache.put(cacheKey, translation.translatedText)

            TranslationResult.Success(
                translatedText = translation.translatedText,
                sourceLanguage = translation.detectedSourceLanguage ?: sourceLanguage,
                isOffline = false
            )
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                400, 401, 403 -> TranslationResult.Error(
                    "API 키 오류 — local.properties에 TRANSLATION_API_KEY를 설정해 주세요."
                )
                else -> TranslationResult.Error("Cloud 번역 서버 오류 (HTTP ${e.code()})")
            }
        } catch (e: Exception) {
            TranslationResult.Error("네트워크 오류: ${e.localizedMessage}", e)
        }
    }
}
