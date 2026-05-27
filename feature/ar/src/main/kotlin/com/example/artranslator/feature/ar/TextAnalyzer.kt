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
 * - AUTO  : 일본어 인식기로 프로브 → 결과 Unicode 분석 → 최초 1회 onScriptDetected 호출
 *           (CJK+가나=일본어, CJK만=중국어, 라틴=영어계열 순으로 판별; 한국어는 수동 선택 권장)
 * - LATIN : 영어·프랑스어·독일어·스페인어 등 라틴 계열
 * - JAPANESE / CHINESE / KOREAN : 해당 ML Kit 인식기 전용
 */
class TextAnalyzer(
    val script: Script = Script.AUTO,
    private val onTextDetected: (blocks: List<TextBlock>, effectiveW: Int, effectiveH: Int) -> Unit,
    /** AUTO 모드에서 스크립트가 감지됐을 때 딱 1회 호출. 이후 caller가 TextAnalyzer를 재생성하세요. */
    private val onScriptDetected: ((Script) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    enum class Script { AUTO, LATIN, KOREAN, JAPANESE, CHINESE }

    data class TextBlock(
        val text: String,
        val boundingBox: Rect?,
        val confidence: Float
    )

    // AUTO → JAPANESE 인식기를 프로브로 사용 (CJK + 가나 감지 최적)
    private val recognizer: TextRecognizer = when (script) {
        Script.AUTO     -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
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
    // AUTO 모드: 최초 1회만 감지 알림
    private val autoDetected = AtomicBoolean(false)

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

                // AUTO 모드: 결과 텍스트에서 스크립트 추론 → 1회만 콜백
                if (script == Script.AUTO && blocks.isNotEmpty() && !autoDetected.get()) {
                    val combined = blocks.joinToString(" ") { it.text }
                    val detected = detectScriptFromText(combined)
                    if (autoDetected.compareAndSet(false, true)) {
                        onScriptDetected?.invoke(detected)
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

    /**
     * 인식된 텍스트의 Unicode 문자 분포로 스크립트를 추론.
     *
     * - 히라가나·가타카나가 5% 이상 → JAPANESE
     * - 한글이 10% 이상             → KOREAN
     * - CJK가 10% 이상             → CHINESE (가나 없음)
     * - 그 외                       → LATIN
     *
     * 주의: AUTO 모드는 JAPANESE 인식기를 프로브로 사용하므로
     * 한국어 텍스트는 인식이 불안정할 수 있습니다 → 수동 선택 권장.
     */
    private fun detectScriptFromText(text: String): Script {
        var kana = 0; var hangul = 0; var cjk = 0; var latin = 0
        for (c in text) {
            val code = c.code
            when {
                code in 0x3040..0x30FF -> kana++              // 히라가나 + 가타카나
                code in 0xAC00..0xD7A3 || code in 0x1100..0x11FF -> hangul++  // 한글
                code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF -> cjk++     // CJK
                c.isLetter() -> latin++
            }
        }
        val total = (kana + hangul + cjk + latin).coerceAtLeast(1)
        return when {
            kana.toFloat() / total > 0.05f   -> Script.JAPANESE
            hangul.toFloat() / total > 0.10f -> Script.KOREAN
            cjk.toFloat() / total > 0.10f    -> Script.CHINESE
            else                              -> Script.LATIN
        }
    }

    fun shutdown() {
        recognizer.close()
    }

    companion object {
        /** Script → 사용자에게 보여줄 이름 */
        fun Script.displayName() = when (this) {
            Script.AUTO     -> "자동"
            Script.LATIN    -> "영어 계열"
            Script.JAPANESE -> "일본어"
            Script.CHINESE  -> "중국어"
            Script.KOREAN   -> "한국어"
        }
    }
}
