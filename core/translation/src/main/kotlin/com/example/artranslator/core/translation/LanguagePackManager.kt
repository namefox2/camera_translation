package com.example.artranslator.core.translation

import com.example.artranslator.core.translation.model.DownloadState
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
 *  - downloadModelIfNeeded(제한 없음 조건) 으로 즉시 다운로드 시작
 *  - Task.isComplete 폴링(2초 간격) → DownloadState.Downloading(elapsedSeconds) 방출
 *    → UI에서 "XX초 경과" 실시간 표시
 *  - 타임아웃 없음 (앱이 살아있는 한 계속 대기)
 *  - 완료 후 isModelDownloaded()로 실제 저장 검증
 */
@Singleton
class LanguagePackManager @Inject constructor() {

    private val modelManager = RemoteModelManager.getInstance()

    fun downloadModel(languageCode: String, requireWifi: Boolean = false): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        val mlKitCode = languageCode.toMlKitCode()
        // WiFi 체크는 ViewModel에서 사전 완료 → ML Kit에는 항상 제한 없음 조건
        val conditions = DownloadConditions.Builder().build()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlKitCode)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
        val translator = Translation.getClient(options)

        try {
            // 다운로드 Task 시작 (await 하지 않고 폴링)
            val downloadTask = translator.downloadModelIfNeeded(conditions)

            // Task가 완료될 때까지 2초 간격으로 경과 시간 방출
            var elapsedSec = 0
            while (!downloadTask.isComplete) {
                delay(2_000L)
                elapsedSec += 2
                emit(DownloadState.Downloading(elapsedSec))
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
