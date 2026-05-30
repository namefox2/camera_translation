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
import java.util.concurrent.atomic.AtomicInteger

/**
 * CameraX ImageAnalysis.Analyzer — ML Kit 온디바이스 텍스트 인식.
 *
 * [Script.AUTO] 모드:
 *   - 4개 인식기(일본어·중국어·한국어·라틴)를 동시에 실행
 *   - 각 결과를 점수화(블록 수 × 스크립트 고유 문자 비율)해 최적 결과 선택
 *   - 스크립트 확정 후 onScriptDetected 1회 호출
 *   → ViewModel이 key()로 TextAnalyzer를 단일 인식기로 교체
 *
 * 수동 선택 모드: 해당 인식기 1개만 사용
 */
class TextAnalyzer(
    val script: Script = Script.AUTO,
    private val onTextDetected: (blocks: List<TextBlock>, effectiveW: Int, effectiveH: Int) -> Unit,
    /** AUTO 모드에서 스크립트가 확정됐을 때 딱 1회 호출 */
    private val onScriptDetected: ((Script) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    enum class Script { AUTO, LATIN, KOREAN, JAPANESE, CHINESE }

    data class TextBlock(
        val text: String,
        val boundingBox: Rect?,
        val confidence: Float
    )

    // AUTO: 4개 인식기 / 수동: 1개
    private val autoRecognizers: Map<Script, TextRecognizer>? = if (script == Script.AUTO) {
        mapOf(
            Script.JAPANESE to TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            Script.CHINESE  to TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
            Script.KOREAN   to TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
            Script.LATIN    to TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        )
    } else null

    private val singleRecognizer: TextRecognizer? = when (script) {
        Script.LATIN    -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        Script.KOREAN   -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        Script.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        Script.CHINESE  -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        Script.AUTO     -> null
    }

    private val isProcessing = AtomicBoolean(false)
    @Volatile private var lastAnalyzedTimestamp = 0L
    private val debounceMs = 600L
    private val autoDetected = AtomicBoolean(false)
    // 스크립트 확정 후 단일 인식기만 사용해 배터리 절약
    @Volatile private var confirmedScript: Script? = null

    @Volatile var paused = false
        private set

    fun pause() { paused = true }
    fun resume() { paused = false; lastAnalyzedTimestamp = 0L }

    // ── 분석 ──────────────────────────────────────────────────────────────────

    override fun analyze(imageProxy: ImageProxy) {
        if (paused) { imageProxy.close(); return }
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

        if (script == Script.AUTO) {
            runAutoRecognizers(inputImage, imageProxy, effectiveW, effectiveH)
        } else {
            runSingleRecognizer(inputImage, imageProxy, effectiveW, effectiveH)
        }
    }

    // ── AUTO 모드: 4개 병렬 실행 ──────────────────────────────────────────────

    private fun runAutoRecognizers(
        inputImage: InputImage,
        imageProxy: ImageProxy,
        effectiveW: Int,
        effectiveH: Int
    ) {
        val recognizers = autoRecognizers ?: run {
            imageProxy.close(); isProcessing.set(false); return
        }

        // 스크립트 확정 후: 4개 동시 실행 대신 해당 인식기 1개만 실행 → CPU/배터리 절약
        val confirmed = confirmedScript
        if (confirmed != null) {
            val recognizer = recognizers[confirmed] ?: recognizers.values.first()
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.flatMap { block ->
                        block.lines.mapNotNull { line ->
                            line.boundingBox?.let { box -> TextBlock(line.text, box, line.confidence) }
                        }
                    }
                    onTextDetected(blocks, effectiveW, effectiveH)
                }
                .addOnFailureListener { onTextDetected(emptyList(), effectiveW, effectiveH) }
                .addOnCompleteListener { imageProxy.close(); isProcessing.set(false) }
            return
        }

        val results = mutableMapOf<Script, com.google.mlkit.vision.text.Text?>()
        val pending = AtomicInteger(recognizers.size)

        recognizers.forEach { (recScript, recognizer) ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    synchronized(results) { results[recScript] = text }
                }
                .addOnFailureListener {
                    synchronized(results) { results[recScript] = null }
                }
                .addOnCompleteListener {
                    // 마지막 Task 완료 시 결과 처리
                    if (pending.decrementAndGet() == 0) {
                        processAutoResults(results, effectiveW, effectiveH)
                        imageProxy.close()
                        isProcessing.set(false)
                    }
                }
        }
    }

    private fun processAutoResults(
        results: Map<Script, com.google.mlkit.vision.text.Text?>,
        effectiveW: Int,
        effectiveH: Int
    ) {
        // 각 인식기 결과를 점수화 → 최고점 선택
        val best = results.entries
            .filter { (_, text) -> text != null && text.textBlocks.isNotEmpty() }
            .maxByOrNull { (recScript, text) -> scoreText(text!!, recScript) }

        if (best == null) {
            onTextDetected(emptyList(), effectiveW, effectiveH)
            return
        }

        val (detectedScript, bestText) = best
        // 단락(textBlock)이 아닌 행(line) 단위로 분리해 촘촘하게 인식
        val blocks = bestText!!.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                line.boundingBox?.let { box ->
                    TextBlock(line.text, box, line.confidence)
                }
            }
        }

        // 스크립트 확정 알림 (1회만)
        if (blocks.isNotEmpty() && !autoDetected.get()) {
            if (autoDetected.compareAndSet(false, true)) {
                confirmedScript = detectedScript
                onScriptDetected?.invoke(detectedScript)
            }
        }

        onTextDetected(blocks, effectiveW, effectiveH)
    }

    /**
     * 인식 결과 점수 = (블록 수 × 10) + 스크립트 고유 문자 수 × 가중치
     *
     * 가중치:
     *  - 히라가나·가타카나: 3.0 (일본어에만 고유)
     *  - 한글:             2.5 (한국어에만 고유)
     *  - CJK(중국어 결과): 2.0
     *  - CJK(일본어 결과): 1.0 (한자는 중·일 공유)
     *  - 라틴 문자:        1.0
     */
    private fun scoreText(text: com.google.mlkit.vision.text.Text, recScript: Script): Double {
        var score = text.textBlocks.size * 2.0

        for (c in text.text) {
            val code = c.code
            score += when (recScript) {
                Script.JAPANESE -> when {
                    code in 0x3040..0x30FF -> 3.0   // 히라가나·가타카나 (고유)
                    code in 0x4E00..0x9FFF -> 1.0   // 한자 (중·일 공유)
                    else -> 0.0
                }
                Script.CHINESE -> when {
                    code in 0x4E00..0x9FFF -> 2.0   // CJK
                    code in 0x3400..0x4DBF -> 1.5   // CJK 확장
                    else -> 0.0
                }
                Script.KOREAN -> when {
                    code in 0xAC00..0xD7A3 -> 2.5   // 한글 (고유)
                    code in 0x1100..0x11FF -> 1.5
                    else -> 0.0
                }
                Script.LATIN -> if (c.isLetter() && code < 0x300) 1.0 else 0.0
                Script.AUTO  -> 0.0
            }
        }
        return score
    }

    // ── 수동 모드: 단일 인식기 ────────────────────────────────────────────────

    private fun runSingleRecognizer(
        inputImage: InputImage,
        imageProxy: ImageProxy,
        effectiveW: Int,
        effectiveH: Int
    ) {
        singleRecognizer!!.process(inputImage)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.flatMap { block ->
                    block.lines.mapNotNull { line ->
                        line.boundingBox?.let { box ->
                            TextBlock(line.text, box, line.confidence)
                        }
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

    // ── 정리 ──────────────────────────────────────────────────────────────────

    fun shutdown() {
        singleRecognizer?.close()
        autoRecognizers?.values?.forEach { it.close() }
    }

    companion object {
        fun Script.displayName() = when (this) {
            Script.AUTO     -> "자동"
            Script.LATIN    -> "영어 계열"
            Script.JAPANESE -> "일본어"
            Script.CHINESE  -> "중국어"
            Script.KOREAN   -> "한국어"
        }
    }
}
