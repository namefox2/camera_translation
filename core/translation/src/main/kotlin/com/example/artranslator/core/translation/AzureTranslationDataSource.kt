package com.example.artranslator.core.translation

import android.util.Log
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
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private interface AzureTranslatorApi {
    @POST("translate")
    suspend fun translate(
        @Query("api-version") apiVersion: String,
        @Query("to") to: String,
        @Query("from") from: String?,
        @Header("Ocp-Apim-Subscription-Key") key: String,
        @Body body: List<AzureTranslateItem>
    ): List<AzureTranslateResult>
}

data class AzureTranslateItem(
    @SerializedName("Text") val text: String
)

data class AzureTranslateResult(
    @SerializedName("translations") val translations: List<AzureTranslation>?,
    @SerializedName("detectedLanguage") val detectedLanguage: AzureDetectedLanguage?
)

data class AzureTranslation(
    @SerializedName("text") val text: String,
    @SerializedName("to") val to: String
)

data class AzureDetectedLanguage(
    @SerializedName("language") val language: String,
    @SerializedName("score") val score: Float
)

@Singleton
class AzureTranslationDataSource @Inject constructor(
    @Named("azure_translation_region") private val region: String
) {

    private val api: AzureTranslatorApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val clientBuilder = OkHttpClient.Builder().addInterceptor(logging)
        if (region.isNotBlank()) {
            clientBuilder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Ocp-Apim-Subscription-Region", region)
                    .build()
                chain.proceed(request)
            }
        }
        Retrofit.Builder()
            .baseUrl("https://api.cognitive.microsofttranslator.com/")
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AzureTranslatorApi::class.java)
    }

    private val cache = LruCache<String, String>(200)

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String?,
        apiKey: String
    ): TranslationResult {
        if (apiKey.isBlank()) {
            Log.w(TAG, "AZURE_TRANSLATION_KEY is blank — check local.properties and do Clean+Rebuild")
            return TranslationResult.Error(
                "Azure API 키가 비어 있습니다.\n" +
                "local.properties에 AZURE_TRANSLATION_KEY=발급받은키 를 추가한 뒤\n" +
                "Android Studio → Build → Clean Project → Rebuild Project 를 실행하세요."
            )
        }
        Log.d(TAG, "translate(): keyLen=${apiKey.length}, region='$region'")

        val targetCode = targetLanguage.toAzureCode()
        val sourceCode = sourceLanguage?.toAzureCode()

        val cacheKey = "$text|${sourceCode ?: "auto"}|$targetCode"
        cache.get(cacheKey)?.let { return TranslationResult.Success(it, sourceLanguage, isOffline = false) }

        return try {
            val results = api.translate(
                apiVersion = "3.0",
                to = targetCode,
                from = sourceCode,
                key = apiKey,
                body = listOf(AzureTranslateItem(text))
            )
            val translation = results.firstOrNull()?.translations?.firstOrNull()
                ?: return TranslationResult.Error("번역 결과가 없습니다.")

            cache.put(cacheKey, translation.text)
            val detectedSrc = results.firstOrNull()?.detectedLanguage?.language ?: sourceLanguage
            TranslationResult.Success(
                translatedText = translation.text,
                sourceLanguage = detectedSrc,
                isOffline = false
            )
        } catch (e: retrofit2.HttpException) {
            Log.w(TAG, "Azure HTTP ${e.code()}: keyLen=${apiKey.length}, region='$region'")
            when (e.code()) {
                400 -> TranslationResult.Error("잘못된 요청입니다.")
                401, 403 -> {
                    val hint = if (region.isBlank())
                        "AZURE_TRANSLATION_REGION이 설정되지 않았습니다. local.properties에 예) AZURE_TRANSLATION_REGION=koreacentral 을 추가해 주세요."
                    else
                        "Azure API 키를 Azure Portal에서 재확인해 주세요. (region=$region)"
                    TranslationResult.Error("Azure 인증 실패 (${e.code()}). $hint")
                }
                429 -> TranslationResult.Error("요청 한도를 초과했습니다. 잠시 후 다시 시도하세요.")
                else -> TranslationResult.Error("Azure 서버 오류 (HTTP ${e.code()})")
            }
        } catch (e: Exception) {
            TranslationResult.Error("네트워크 오류: ${e.localizedMessage}", e)
        }
    }

    companion object { private const val TAG = "AzureTranslator" }

    private fun String.toAzureCode(): String = when (this.lowercase()) {
        "ko" -> "ko"
        "en" -> "en"
        "ja" -> "ja"
        "zh", "zh-cn" -> "zh-Hans"
        "zh-tw" -> "zh-Hant"
        "fr" -> "fr"
        "de" -> "de"
        "es" -> "es"
        "it" -> "it"
        "pt" -> "pt"
        "ru" -> "ru"
        "id" -> "id"
        "tr" -> "tr"
        "ar" -> "ar"
        "nl" -> "nl"
        "pl" -> "pl"
        "th" -> "th"
        "vi" -> "vi"
        "hi" -> "hi"
        else -> this
    }
}
