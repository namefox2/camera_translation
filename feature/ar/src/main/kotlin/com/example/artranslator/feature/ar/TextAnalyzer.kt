package com.example.artranslator.feature.ar

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX ImageAnalysis.Analyzer — ML Kit 온디바이스 텍스트 인식.
 *
 * [script]에 맞는 인식기를 생성자에서 한 번만 생성.
 * 스크립트 변경이 필요하면 Camera 재바인딩 (key() 방식)으로 새 인스턴스를 만드세요.
 *
 * 지원 스크립트:
 *  - LATIN  : 영어, 프랑스어, 독일어, 스페인어 등 라틴 계열 (기본값)
 *  - KOREAN : 한국어
 *  - JAPANESE: 일본어 (히라가나 · 가타카나 · 한자)
 *  - CHINESE : 중국어 간체/번체
 */
class TextAnalyzer(
    val script: Script = Script.LATIN,
    private val onTextDetected: (blocks: List<TextBlock>, effectiveW: Int, effectiveH: Int) -> Unit
) : ImageAnalysis.Analyzer {

    enum class Script { LATIN, KOREAN, JAPANESE, CHINESE }

    data class TextBlock(
        val text: String,
        val boundingBox: Rect?,
        val confidence: Float
    )

    private val recognizer: TextRecognizer = when (script) {
        Script.LATIN    -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        Script.KOREAN   -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        Script.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        Script.CHINESE  -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    private val isProcessing = AtomicBoolean(false)
    private var lastAnalyzedTimestamp = 0L
    // 1000 ms: Cloud API 번역(~300–600 ms)이 완료될 시간 확보
    // 300 ms로 두면 번역 Job이 항상 취소되어 오버레이가 절대 안 나타남
    private val debounceMs = 1000L

    override fun analyze(imageProxy: ImageProxy) {
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

        // rotation을 반영한 유효 이미지 크기 (ML Kit 좌표계와 일치)
        val rotation = imageProxy.imageInfo.rotationDegrees
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
            .addOnFailureListener { /* 인식 실패 무시 */ }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    fun shutdown() {
        recognizer.close()
    }
}
