package com.letsgo.translator.core.translation

import com.letsgo.translator.core.translation.model.DownloadState
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit 오프라인 번역 모델의 다운로드·삭제·상태 확인.
 *
 * 진행 방식:
 *  - requireWifi=true  → DownloadConditions.requireWifi() — ML Kit WiFi 최적화 경로 사용
 *  - requireWifi=false → DownloadConditions 제한 없음 — 모바일 데이터 허용
 *  - Task.isComplete 폴링(2초 간격) → DownloadState.Downloading(elapsedSeconds) 방출
 *    → UI에서 "XX초 경과" 실시간 표시
 *  - 타임아웃: WiFi 10분 / 모바일 데이터 15분 초과 시 Error 방출
 *  - 완료 후 isModelDownloaded()로 실제 저장 검증
 */
@Singleton
class LanguagePackManager @Inject constructor() {

    private val modelManager = RemoteModelManager.getInstance()

    fun downloadModel(languageCode: String, requireWifi: Boolean = false): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        val mlKitCode = languageCode.toMlKitCode()

        // WiFi 선택 시 ML Kit에도 WiFi 조건 전달
        //  → ML Kit 내부 WiFi 최적화 다운로드 경로를 사용하게 됨
        // 모바일 데이터 선택 시 제한 없음 조건 (이전과 동일)
        val conditions = if (requireWifi) {
            DownloadConditions.Builder().requireWifi().build()
        } else {
            DownloadConditions.Builder().build()
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlKitCode)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
        val translator = Translation.getClient(options)

        try {
            // 다운로드 Task 시작 (await 하지 않고 폴링)
            val downloadTask = translator.downloadModelIfNeeded(conditions)

            // 타임아웃: 모바일 데이터 15분, WiFi 10분
            // (모바일 데이터로 150 MB → 느린 LTE 기준 ~10분; 15분 초과 시 실질적으로 멈춘 것)
            val timeoutSec = if (requireWifi) 600 else 900
            var elapsedSec = 0
            while (!downloadTask.isComplete) {
                delay(2_000L)
                elapsedSec += 2
                emit(DownloadState.Downloading(elapsedSec))

                if (elapsedSec >= timeoutSec) {
                    val msg = if (requireWifi) {
                        "다운로드 시간 초과 (${timeoutSec / 60}분).\n네트워크 상태를 확인 후 다시 시도해 주세요."
                    } else {
                        "모바일 데이터 다운로드 시간 초과 (${timeoutSec / 60}분).\n" +
                        "와이파이로 전환 후 다시 시도하면 훨씬 빠릅니다."
                    }
                    emit(DownloadState.Error(msg))
                    return@flow
                }
            }

            // Task 결과 확인
            if (downloadTask.isSuccessful) {
                // 실제 모델 저장 여부 검증
                val verifyModel = TranslateRemoteModel.Builder(mlKitCode).build()
                val isReady = try {
                    modelManager.isModelDownloaded(verifyModel).await()
                } catch (_: Exception) {
                    true // 검증 자체가 실패하면 Task 성공 기준으로 판단
                }
                if (isReady) {
                    emit(DownloadState.Downloaded)
                } else {
                    emit(DownloadState.Error(
                        "다운로드 완료됐지만 모델을 찾을 수 없습니다.\n" +
                        "저장공간이 부족하거나 Play Services 문제일 수 있습니다."
                    ))
                }
            } else {
                val errMsg = downloadTask.exception?.message
                    ?: downloadTask.exception?.localizedMessage
                    ?: "알 수 없는 오류"
                emit(DownloadState.Error("다운로드 실패: $errMsg"))
            }
        } catch (e: Exception) {
            emit(DownloadState.Error("오류: ${e.message ?: e.javaClass.simpleName}"))
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
