package com.example.artranslator.core.translation

import com.example.artranslator.core.translation.model.DownloadState
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit 오프라인 번역 모델의 다운로드·삭제·상태 확인을 담당합니다.
 *
 * 핵심 설계 원칙:
 *  - ML Kit Translate의 공식 다운로드 API: translator.downloadModelIfNeeded(conditions)
 *  - conditions에는 항상 DownloadConditions.Builder().build() (제한 없음) 을 전달합니다.
 *    → ML Kit 내부에서 "WiFi 대기" 로직이 동작하는 것을 막아 무한 스피너 방지.
 *  - WiFi 여부 체크는 LanguageManagerViewModel에서 사전 처리하므로 여기서는 불필요.
 *  - 120초 타임아웃으로 만약의 hang 방지.
 *  - isModelAvailable()은 RemoteModelManager.isModelDownloaded()로 정확히 확인.
 */
@Singleton
class LanguagePackManager @Inject constructor() {

    private val modelManager = RemoteModelManager.getInstance()

    /**
     * 언어 모델을 다운로드합니다.
     * WiFi 체크는 ViewModel에서 사전 완료된 상태이므로 ML Kit에는 조건 없이 전달합니다.
     */
    fun downloadModel(languageCode: String, requireWifi: Boolean = false): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        val mlKitCode = languageCode.toMlKitCode()
        // ML Kit에 WiFi 조건을 넘기지 않음 → 내부 "WiFi 대기" 로직 차단
        // WiFi 체크는 LanguageManagerViewModel.confirmDownload()에서 이미 완료
        val conditions = DownloadConditions.Builder().build()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlKitCode)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
        val translator = Translation.getClient(options)

        try {
            withTimeout(120_000L) {
                // 공식 API: downloadModelIfNeeded(conditions) 에 명시적 조건 전달
                // 조건 없이 호출하면 ML Kit가 내부적으로 WiFi를 기다려 영원히 pending 가능
                translator.downloadModelIfNeeded(conditions).await()
            }
            emit(DownloadState.Downloaded)
        } catch (e: TimeoutCancellationException) {
            emit(DownloadState.Error("다운로드 시간이 초과되었습니다 (2분). 네트워크를 확인해 주세요."))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.localizedMessage ?: "다운로드에 실패했습니다"))
        } finally {
            translator.close()
        }
    }

    suspend fun deleteModel(languageCode: String): Boolean {
        return try {
            val model = TranslateRemoteModel.Builder(languageCode.toMlKitCode()).build()
            modelManager.deleteDownloadedModel(model).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** RemoteModelManager로 실제 다운로드 여부를 확인합니다 (probe 번역 방식 제거). */
    suspend fun isModelAvailable(languageCode: String): Boolean {
        return try {
            val model = TranslateRemoteModel.Builder(languageCode.toMlKitCode()).build()
            modelManager.isModelDownloaded(model).await()
        } catch (e: Exception) {
            false
        }
    }

    fun String.toMlKitCode(): String = when (this.lowercase()) {
        "ko" -> TranslateLanguage.KOREAN
        "en" -> TranslateLanguage.ENGLISH
        "ja" -> TranslateLanguage.JAPANESE
        "zh", "zh-cn", "zh-tw" -> TranslateLanguage.CHINESE
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
