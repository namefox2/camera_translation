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
     *
     * 다운로드 흐름:
     * 1. downloadModelIfNeeded(조건 없음) → ML Kit가 즉시 다운로드 시작
     * 2. Task 완료 후 isModelDownloaded()로 실제 저장 여부 검증
     * 3. 검증 실패 시 명시적 에러 발생
     *
     * WiFi 체크는 ViewModel에서 사전 완료 → ML Kit에는 "제한 없음" 조건만 전달
     */
    fun downloadModel(languageCode: String, requireWifi: Boolean = false): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        val mlKitCode = languageCode.toMlKitCode()
        // ML Kit에는 항상 네트워크 제한 없는 조건 전달
        // (WiFi 체크는 ViewModel에서 이미 완료, ML Kit 내부 "WiFi 대기" 방지)
        val conditions = DownloadConditions.Builder().build()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlKitCode)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
        val translator = Translation.getClient(options)

        try {
            withTimeout(120_000L) {
                translator.downloadModelIfNeeded(conditions).await()
            }

            // ─ 다운로드 완료 검증 ──────────────────────────────────────────────
            // 일부 ML Kit 버전에서 Task가 실제 완료 전에 success를 반환하는 케이스 존재
            val verifyModel = TranslateRemoteModel.Builder(mlKitCode).build()
            val isReady = try {
                modelManager.isModelDownloaded(verifyModel).await()
            } catch (_: Exception) {
                // isModelDownloaded 자체가 실패하면 Task가 성공했으므로 true로 간주
                true
            }

            if (isReady) {
                emit(DownloadState.Downloaded)
            } else {
                emit(DownloadState.Error(
                    "모델을 다운로드했지만 확인할 수 없습니다.\n" +
                    "저장공간 부족이나 Play Services 문제일 수 있습니다."
                ))
            }
        } catch (e: TimeoutCancellationException) {
            emit(DownloadState.Error(
                "다운로드 시간 초과 (2분)\n인터넷 연결 또는 속도를 확인해 주세요."
            ))
        } catch (e: Exception) {
            // ML Kit 에러 메시지를 그대로 표시해 진단 가능하게
            val msg = e.message ?: e.localizedMessage ?: e.javaClass.simpleName
            emit(DownloadState.Error("다운로드 오류: $msg"))
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
