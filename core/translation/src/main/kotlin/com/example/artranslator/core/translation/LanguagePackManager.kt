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
 * 이전 구현의 문제:
 *  - TranslatorOptions + downloadModelIfNeeded() 는 내부적으로 WiFi 조건이 맞지 않으면
 *    Task가 영원히 pending 상태로 남아 Flow가 complete되지 않음 → UI 무한 로딩
 *  - probe 번역("test")으로 isModelDownloaded 판단 → 모델 없으면 다운로드 시도해 꼬임
 *
 * 수정:
 *  - TranslateRemoteModel + RemoteModelManager.download() 로 직접 제어
 *  - RemoteModelManager.isModelDownloaded() 로 상태 정확히 확인
 *  - 120초 타임아웃 + 명확한 에러 메시지
 *  - WiFi 미요구 (모바일 데이터에서도 다운로드 가능)
 */
@Singleton
class LanguagePackManager @Inject constructor() {

    private val modelManager = RemoteModelManager.getInstance()

    fun downloadModel(languageCode: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))
        try {
            val mlKitCode = languageCode.toMlKitCode()

            // 소스→한국어 모델 다운로드 (한국어 사용자 기준)
            val sourceModel = TranslateRemoteModel.Builder(mlKitCode).build()
            // 한국어→소스 모델도 함께 다운로드 (양방향 통역 지원)
            val targetModel = TranslateRemoteModel.Builder(TranslateLanguage.KOREAN).build()

            val conditions = DownloadConditions.Builder().build() // WiFi 미요구

            withTimeout(120_000L) {
                // 두 방향 모델을 순서대로 다운로드
                modelManager.download(sourceModel, conditions).await()
                // 한국어 모델은 이미 있을 가능성이 높지만 안전하게 재시도
                runCatching { modelManager.download(targetModel, conditions).await() }
            }

            emit(DownloadState.Downloaded)
        } catch (e: TimeoutCancellationException) {
            emit(DownloadState.Error("다운로드 시간이 초과되었습니다 (2분). 네트워크를 확인해 주세요."))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.localizedMessage ?: "다운로드에 실패했습니다"))
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
