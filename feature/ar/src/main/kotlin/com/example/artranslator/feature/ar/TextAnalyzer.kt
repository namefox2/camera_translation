package com.example.artranslator.feature.ar

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX [ImageAnalysis.Analyzer] that performs on-device text recognition
 * using ML Kit. Detected text blocks are passed to [onTextDetected].
 *
 * Performance notes:
 *  - 300 ms debounce to avoid excessive OCR on every frame
 *  - ML Kit Text Recognizer is closed properly to prevent memory leaks
 */
class TextAnalyzer(
    private val onTextDetected: (List<TextBlock>) -> Unit
) : ImageAnalysis.Analyzer {

    data class TextBlock(
        val text: String,
        val boundingBox: Rect?,
        val confidence: Float
    )

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val analyzerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isProcessing = AtomicBoolean(false)

    // 300 ms debounce: timestamp of last accepted frame
    private var lastAnalyzedTimestamp = 0L
    private val debounceMs = 300L

    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalyzedTimestamp < debounceMs) {
            imageProxy.close()
            return
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        lastAnalyzedTimestamp = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

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
                onTextDetected(blocks)
            }
            .addOnFailureListener {
                // Silently ignore recognition errors
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    fun shutdown() {
        recognizer.close()
        analyzerScope.cancel()
    }
}
