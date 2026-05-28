package com.example.artranslator.core.translation

import android.util.LruCache
import com.example.artranslator.core.translation.model.TranslationResult
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

private interface DeepLApi {
    @POST("v2/translate")
    suspend fun translate(
        @Header("Authorization") authHeader: String,
        @Body request: DeepLRequest
    ): DeepLResponse
}

data class DeepLRequest(
    @SerializedName("text") val text: List<String>,
    @SerializedName("target_lang") val targetLang: String,
    @SerializedName("source_lang") val sourceLang: String? = null
)

data class DeepLResponse(
    @SerializedName("translations") val translations: List<DeepLTranslation>?
)

data class DeepLTranslation(
    @SerializedName("text") val text: String,
    @SerializedName("detected_source_language") val detectedSourceLanguage: String?
)

@Singleton
class DeepLTranslationDataSource @Inject constructor() {

    private val api: DeepLApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl("https://api-free.deepl.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepLApi::class.java)
    }

    private val cache = LruCache<String, String>(200)

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String?,
        apiKey: String
    ): TranslationResult {
        if (apiKey.isBlank()) {
            return TranslationResult.Error("DeepL API 키가 설정되지 않았습니다. local.properties에 DEEPL_API_KEY를 추가해 주세요.")
        }

        val targetCode = targetLanguage.toDeepLCode()
            ?: return TranslationResult.Error("DeepL이 지원하지 않는 언어입니다: $targetLanguage")
        val sourceCode = sourceLanguage?.toDeepLCode()

        val cacheKey = "$text|${sourceCode ?: "auto"}|$targetCode"
        cache.get(cacheKey)?.let { return TranslationResult.Success(it, sourceLanguage, isOffline = false) }

        return try {
            val response = api.translate(
                authHeader = "DeepL-Auth-Key $apiKey",
                request = DeepLRequest(
                    text = listOf(text),
                    targetLang = targetCode,
                    sourceLang = sourceCode
                )
            )
            val translation = response.translations?.firstOrNull()
                ?: return TranslationResult.Error("번역 결과가 없습니다.")

            cache.put(cacheKey, translation.text)
            TranslationResult.Success(
                translatedText = translation.text,
                sourceLanguage = translation.detectedSourceLanguage?.lowercase() ?: sourceLanguage,
                isOffline = false
            )
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                400 -> TranslationResult.Error("잘못된 요청입니다.")
                401, 403 -> TranslationResult.Error("DeepL API 키가 올바르지 않습니다. local.properties를 확인해 주세요.")
                456 -> TranslationResult.Error("DeepL 무료 한도(50만 자/월)를 초과했습니다.")
                else -> TranslationResult.Error("DeepL 서버 오류 (HTTP ${e.code()})")
            }
        } catch (e: Exception) {
            TranslationResult.Error("네트워크 오류: ${e.localizedMessage}", e)
        }
    }

    // DeepL 미지원 언어는 null 반환
    private fun String.toDeepLCode(): String? = when (this.lowercase()) {
        "ko" -> "KO"
        "en" -> "EN"
        "ja" -> "JA"
        "zh", "zh-cn", "zh-tw" -> "ZH"
        "fr" -> "FR"
        "de" -> "DE"
        "es" -> "ES"
        "it" -> "IT"
        "pt" -> "PT"
        "ru" -> "RU"
        "id" -> "ID"
        "tr" -> "TR"
        "ar" -> "AR"
        "nl" -> "NL"
        "pl" -> "PL"
        else -> null  // Thai(th), Vietnamese(vi), Hindi(hi) 등 미지원
    }
}
