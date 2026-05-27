package com.example.artranslator.feature.ar

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artranslator.core.translation.TranslationRepository
import com.example.artranslator.core.translation.model.TranslationResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ─── UI 상태 ───────────────────────────────────────────────────────────────────

sealed class CaptureStep {
    /** 카메라 프리뷰 단계 */
    object Preview : CaptureStep()

    /** 사진 촬영 후 영역 선택 단계 */
    data class Selecting(val bitmap: Bitmap) : CaptureStep()

    /** 번역 완료 단계 */
    data class Result(
        val bitmap: Bitmap,
        val selStart: Offset,
        val selEnd: Offset,
        val recognizedText: String,
        val translatedText: String,
        val isOffline: Boolean
    ) : CaptureStep()
}

data class CameraTranslateUiState(
    val step: CaptureStep = CaptureStep.Preview,
    val isProcessing: Boolean = false,
    val targetLanguage: String = "ko",
    val sourceScript: TextAnalyzer.Script = TextAnalyzer.Script.AUTO,  // 기본값: 자동 감지
    val error: String? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class CameraTranslateViewModel @Inject constructor(
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraTranslateUiState())
    val uiState: StateFlow<CameraTranslateUiState> = _uiState.asStateFlow()

    /** CameraX ImageCapture 콜백에서 Bitmap을 받으면 호출 */
    fun onPhotoCaptured(bitmap: Bitmap, rotationDegrees: Int = 0) {
        val rotated = if (rotationDegrees != 0) {
            val m = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        } else {
            bitmap
        }
        _uiState.update { it.copy(step = CaptureStep.Selecting(rotated), error = null) }
    }

    /** 사용자가 드래그한 선택 영역으로 OCR + 번역 실행 */
    fun translateSelection(
        bitmap: Bitmap,
        selStart: Offset,
        selEnd: Offset,
        viewSize: IntSize
    ) {
        if (abs(selEnd.x - selStart.x) < 10f || abs(selEnd.y - selStart.y) < 10f) {
            _uiState.update { it.copy(error = "영역을 더 크게 드래그해 주세요") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }

            try {
                // 1. 선택 영역을 비트맵 좌표로 변환 후 크롭
                val cropped = cropBitmap(bitmap, selStart, selEnd, viewSize)

                // 2. ML Kit OCR — AUTO면 Japanese 프로브 → 스크립트 감지 → 필요시 재인식
                val recognized = recognizeWithAutoDetect(cropped, _uiState.value.sourceScript)
                if (recognized == null) {
                    _uiState.update { it.copy(isProcessing = false, error = "선택한 영역에서 텍스트를 찾지 못했습니다.\n다른 영역을 선택해 주세요.") }
                    return@launch
                }

                // 3. 번역
                val result = translationRepository.translate(
                    text = recognized,
                    targetLanguage = _uiState.value.targetLanguage
                )

                when (result) {
                    is TranslationResult.Success -> _uiState.update {
                        it.copy(
                            isProcessing = false,
                            step = CaptureStep.Result(
                                bitmap = bitmap,
                                selStart = selStart,
                                selEnd = selEnd,
                                recognizedText = recognized,
                                translatedText = result.translatedText,
                                isOffline = result.isOffline
                            )
                        )
                    }
                    is TranslationResult.Error -> _uiState.update {
                        it.copy(isProcessing = false, error = result.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = "오류: ${e.localizedMessage}") }
            }
        }
    }

    fun retake() = _uiState.update { it.copy(step = CaptureStep.Preview, error = null) }

    fun reselect(bitmap: Bitmap) = _uiState.update {
        it.copy(step = CaptureStep.Selecting(bitmap), error = null)
    }

    fun setTargetLanguage(code: String) = _uiState.update { it.copy(targetLanguage = code) }

    fun setSourceScript(script: TextAnalyzer.Script) = _uiState.update { it.copy(sourceScript = script) }

    /**
     * AUTO 모드: Japanese 인식기로 프로브 → Unicode 분포로 스크립트 감지 → 필요시 올바른 인식기로 재인식.
     * 수동 선택 모드: 지정된 인식기 1회 실행.
     * @return 인식된 텍스트, 없으면 null
     */
    private suspend fun recognizeWithAutoDetect(
        bitmap: Bitmap,
        script: TextAnalyzer.Script
    ): String? {
        if (script != TextAnalyzer.Script.AUTO) {
            // 수동: 선택한 인식기 그대로 사용
            val recognizer = script.toRecognizer()
            val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
            recognizer.close()
            return text.ifBlank { null }
        }

        // AUTO: 1패스 — Japanese 인식기로 프로브
        val probeRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        val probeText = probeRecognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
        probeRecognizer.close()

        val detectedScript = detectScriptFromText(probeText)

        // 감지된 스크립트가 JAPANESE라면 프로브 결과를 그대로 사용
        if (detectedScript == TextAnalyzer.Script.JAPANESE || probeText.isNotBlank() &&
            detectedScript != TextAnalyzer.Script.LATIN) {
            // CHINESE / KOREAN은 전용 인식기로 재인식해 정확도 향상
            if (detectedScript == TextAnalyzer.Script.CHINESE || detectedScript == TextAnalyzer.Script.KOREAN) {
                val finalRecognizer = detectedScript.toRecognizer()
                val finalText = finalRecognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
                finalRecognizer.close()
                return finalText.ifBlank { probeText.ifBlank { null } }
            }
            return probeText.ifBlank { null }
        }

        // LATIN으로 감지되면 Latin 인식기로 재인식
        val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val latinText = latinRecognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
        latinRecognizer.close()
        return latinText.ifBlank { null }
    }

    private fun TextAnalyzer.Script.toRecognizer(): TextRecognizer =
        when (this) {
            TextAnalyzer.Script.LATIN    -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            TextAnalyzer.Script.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            TextAnalyzer.Script.CHINESE  -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            TextAnalyzer.Script.KOREAN   -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            TextAnalyzer.Script.AUTO     -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        }

    /** TextAnalyzer와 동일한 스크립트 감지 휴리스틱 */
    private fun detectScriptFromText(text: String): TextAnalyzer.Script {
        var kana = 0; var hangul = 0; var cjk = 0; var latin = 0
        for (c in text) {
            val code = c.code
            when {
                code in 0x3040..0x30FF -> kana++
                code in 0xAC00..0xD7A3 || code in 0x1100..0x11FF -> hangul++
                code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF -> cjk++
                c.isLetter() -> latin++
            }
        }
        val total = (kana + hangul + cjk + latin).coerceAtLeast(1)
        return when {
            kana.toFloat() / total > 0.05f   -> TextAnalyzer.Script.JAPANESE
            hangul.toFloat() / total > 0.10f -> TextAnalyzer.Script.KOREAN
            cjk.toFloat() / total > 0.10f    -> TextAnalyzer.Script.CHINESE
            else                              -> TextAnalyzer.Script.LATIN
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ─── 좌표 변환 & 크롭 ─────────────────────────────────────────────────────

    /**
     * ContentScale.Fit으로 표시된 이미지 위의 화면 좌표를
     * 원본 비트맵 픽셀 좌표로 변환한 뒤 크롭합니다.
     */
    private fun cropBitmap(
        bitmap: Bitmap,
        start: Offset,
        end: Offset,
        viewSize: IntSize
    ): Bitmap {
        val scaleX = viewSize.width.toFloat() / bitmap.width
        val scaleY = viewSize.height.toFloat() / bitmap.height
        val scale = min(scaleX, scaleY)                 // ContentScale.Fit

        val displayedW = bitmap.width * scale
        val displayedH = bitmap.height * scale
        val offsetX = (viewSize.width - displayedW) / 2f
        val offsetY = (viewSize.height - displayedH) / 2f

        fun toX(sx: Float) = ((sx - offsetX) / scale).coerceIn(0f, bitmap.width.toFloat())
        fun toY(sy: Float) = ((sy - offsetY) / scale).coerceIn(0f, bitmap.height.toFloat())

        val left   = toX(min(start.x, end.x)).toInt()
        val top    = toY(min(start.y, end.y)).toInt()
        val right  = toX(max(start.x, end.x)).toInt()
        val bottom = toY(max(start.y, end.y)).toInt()
        val w = (right - left).coerceAtLeast(1)
        val h = (bottom - top).coerceAtLeast(1)

        return Bitmap.createBitmap(bitmap, left, top, w, h)
    }
}
