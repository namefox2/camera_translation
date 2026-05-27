package com.example.artranslator.feature.ar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

/**
 * Custom [View] that draws translation bubble overlays on top of the camera preview.
 *
 * Performance notes:
 *  - Paint objects pre-initialised; never created in onDraw()
 *  - Path reused across draw calls
 *  - Thread-safe: call [updateBlocks] from any thread; invalidate posted to main
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class TranslatedBlock(
        val originalText: String,
        val translatedText: String,
        val boundingBox: RectF,
        /** Normalised coordinates [0..1] relative to camera frame */
        val normRect: RectF
    )

    // ─── Paints (pre-initialised) ──────────────────────────────────────────────

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xAA534AB7.toInt()  // default: purple semi-transparent
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFFFFFFFF.toInt()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt()
        textSize = 26f
    }

    private val cornerRadius = 16f
    private val padding = 12f
    private val reusedPath = Path()
    private val reusedRect = RectF()

    // ─── State ─────────────────────────────────────────────────────────────────

    @Volatile
    private var blocks: List<TranslatedBlock> = emptyList()

    /** Camera frame dimensions (original, before scaling to view) */
    private var frameWidth: Int = 1
    private var frameHeight: Int = 1

    // ─── Public API ────────────────────────────────────────────────────────────

    fun updateBlocks(
        newBlocks: List<TranslatedBlock>,
        frameW: Int = frameWidth,
        frameH: Int = frameHeight
    ) {
        blocks = newBlocks
        frameWidth = frameW
        frameHeight = frameH
        post { invalidate() }
    }

    /** Update overlay bubble color to match the active theme. */
    fun setOverlayColor(color: Int) {
        bubblePaint.color = color
        post { invalidate() }
    }

    fun clearOverlay() {
        blocks = emptyList()
        post { invalidate() }
    }

    // ─── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentBlocks = blocks
        if (currentBlocks.isEmpty()) return

        val scaleX = width.toFloat() / frameWidth
        val scaleY = height.toFloat() / frameHeight

        for (block in currentBlocks) {
            val scaledRect = RectF(
                block.normRect.left * width,
                block.normRect.top * height,
                block.normRect.right * width,
                block.normRect.bottom * height
            )

            // Expand rect to fit translated text
            val expandedRect = RectF(
                scaledRect.left - padding,
                scaledRect.top - padding,
                scaledRect.right + padding,
                scaledRect.bottom + padding * 3 + textPaint.textSize
            )

            // Draw bubble background
            reusedPath.reset()
            reusedPath.addRoundRect(expandedRect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.drawPath(reusedPath, bubblePaint)
            canvas.drawPath(reusedPath, strokePaint)

            // Draw translated text
            val textX = expandedRect.left + padding
            val textY = expandedRect.top + padding + textPaint.textSize
            canvas.drawText(
                block.translatedText.take(30),
                textX,
                textY,
                textPaint
            )

            // Draw original text (smaller, below)
            canvas.drawText(
                block.originalText.take(30),
                textX,
                textY + subTextPaint.textSize + 4f,
                subTextPaint
            )
        }
    }
}
