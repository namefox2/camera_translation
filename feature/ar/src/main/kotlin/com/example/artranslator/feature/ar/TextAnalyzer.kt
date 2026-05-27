package com.example.artranslator.feature.ar

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX ImageAnalysis.Analyzer — ML Kit 온디바이스 텍스트 인식.
 *
 * - 300ms 디바운스
 * - 언어별 전용 인식기 지원 (라틴/한국어/일본어/중국어)
 * - 콜백에 이미지 유효 크기(rotation 반영) 포함 → OverlayView 좌표 변환에 사용
 */
class TextAnalyzer(
    private val onTextDetected: (blocks: List<TextBlock>, effectiveW: Int, effectiveH: Int) -> Unit
) : ImageAnalysis.Analyzer {

    enum class Script { LATIN, KOREAN, JAPANESE, CHINESE }

    data class TextBlock(
        val text: String,
        val boundingBox: Rect?,
        val confidence: Float
    )

    // 현재 사용 중인 인식기 (언어 변경 시 교체)
    @Volatile private var recognizer: TextRecognizer = createRecognizer(Script.LATIN)
    @Volatile private var pendingScript: Script? = null

    private val analyzerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isProcessing = AtomicBoolean(false)
    private var lastAnalyzedTimestamp = 0L
    private val debounceMs = 300L

    /** 인식 스크립트 변경 (메인 스레드에서 호출 가능) */
    fun setScript(script: Script) {
        pendingScript = script
    }

    override fun analyze(imageProxy: ImageProxy) {
        // 스크립트 교체가 요청된 경우 (백그라운드 스레드에서 안전하게 처리)
        pendingScript?.let { script ->
            pendingScript = null
            recognizer.close()
            recognizer = createRecognizer(script)
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalyzedTimestamp < debounceMs) {
            imageProxy.close(); return
        }
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close(); return
        }
        lastAnalyzedTimestamp = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close(); isProcessing.set(false); return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        // ML Kit가 rotation을 적용 후 좌표를 반환하므로, 유효 크기도 rotation 반영
        val effectiveW = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val effectiveH = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.mapNotNull { block ->
                    block.boundingBox?.let { box ->
                        TextBlock(
                            text = block.text,
                            boundingBox = box,
                            confidence = block.lines.firstOrNull()?.confidence ?: 0f
                        )
                    }
                }
                onTextDetected(blocks, effectiveW, effectiveH)
            }
            .addOnFailureListener { /* 인식 실패는 무시 */ }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    fun shutdown() {
        recognizer.close()
        analyzerScope.cancel()
    }

    companion object {
        private fun createRecognizer(script: Script): TextRecognizer = when (script) {
            Script.LATIN    -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            Script.KOREAN   -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            Script.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            Script.CHINESE  -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        }
    }
}
