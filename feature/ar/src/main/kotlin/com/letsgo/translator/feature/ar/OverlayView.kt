package com.letsgo.translator.feature.ar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class TranslatedBlock(
        val originalText: String,
        val translatedText: String,
        val normRect: RectF
    )

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xCC534AB7.toInt()
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0x99FFFFFF.toInt()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = 40f
        setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }

    private val textBounds = Rect()

    @Volatile private var blocks: List<TranslatedBlock> = emptyList()
    private var frameWidth: Int = 1
    private var frameHeight: Int = 1

    fun updateBlocks(newBlocks: List<TranslatedBlock>, frameW: Int = frameWidth, frameH: Int = frameHeight) {
        blocks = newBlocks
        frameWidth = frameW
        frameHeight = frameH
        post { invalidate() }
    }

    fun setOverlayColor(color: Int) {
        bgPaint.color = (color and 0x00FFFFFF) or 0xCC000000.toInt()
        post { invalidate() }
    }

    fun clearOverlay() {
        blocks = emptyList()
        post { invalidate() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentBlocks = blocks
        if (currentBlocks.isEmpty() || width == 0 || height == 0) return

        val scaleX = width.toFloat() / frameWidth
        val scaleY = height.toFloat() / frameHeight
        val scale = maxOf(scaleX, scaleY)
        val displayedW = frameWidth * scale
        val displayedH = frameHeight * scale
        val offsetX = (width - displayedW) / 2f
        val offsetY = (height - displayedH) / 2f

        for (block in currentBlocks) {
            val vLeft   = block.normRect.left   * displayedW + offsetX
            val vTop    = block.normRect.top    * displayedH + offsetY
            val vRight  = block.normRect.right  * displayedW + offsetX
            val vBottom = block.normRect.bottom * displayedH + offsetY

            val viewRect = RectF(vLeft, vTop, vRight, vBottom)
            if (!viewRect.intersect(0f, 0f, width.toFloat(), height.toFloat())) continue
            if (viewRect.width() < 8f || viewRect.height() < 8f) continue

            val displayText = block.translatedText.ifBlank { block.originalText }

            // 배경 그리기
            canvas.drawRoundRect(viewRect, 8f, 8f, bgPaint)
            canvas.drawRoundRect(viewRect, 8f, 8f, strokePaint)

            val isVertical = viewRect.height() > viewRect.width() * 1.6f

            if (isVertical) {
                drawVerticalText(canvas, displayText, viewRect)
            } else {
                drawHorizontalText(canvas, displayText, viewRect)
            }
        }
    }

    /**
     * 세로로 긴 블록 (일본어·중국어 세로쓰기 등): 캔버스를 -90° 회전해서 텍스트 출력.
     * 회전 후 가용 폭 = 블록 높이, 가용 높이 = 블록 폭.
     */
    private fun drawVerticalText(canvas: Canvas, text: String, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()

        canvas.save()
        canvas.rotate(-90f, cx, cy)

        // 회전 공간에서 가로폭 = 원래 높이, 세로폭 = 원래 너비
        val availW = rect.height() - 8f
        val availH = rect.width()

        textPaint.textSize = (availH * 0.65f).coerceIn(18f, 56f)

        val measuredW = textPaint.measureText(text)
        if (measuredW > availW && measuredW > 0f) {
            textPaint.textSize *= availW / measuredW
            textPaint.textSize = textPaint.textSize.coerceAtLeast(16f)
        }

        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textX = cx - textPaint.measureText(text) / 2f
        val textY = cy + textBounds.height() / 2f - textPaint.descent()
        canvas.drawText(text, textX, textY, textPaint)

        canvas.restore()
    }

    /**
     * 가로형 블록: 단일 행 기준으로 폭에 맞게 폰트 크기 조정.
     */
    private fun drawHorizontalText(canvas: Canvas, text: String, rect: RectF) {
        val availW = rect.width() - 10f
        val availH = rect.height()

        textPaint.textSize = (availH * 0.60f).coerceIn(18f, 56f)

        val measuredW = textPaint.measureText(text)
        if (measuredW > availW && measuredW > 0f) {
            textPaint.textSize *= availW / measuredW
            textPaint.textSize = textPaint.textSize.coerceAtLeast(16f)
        }

        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textX = rect.left + (rect.width() - textPaint.measureText(text)) / 2f
        val textY = rect.centerY() + textBounds.height() / 2f - textPaint.descent()
        canvas.drawText(text, textX, textY, textPaint)
    }
}
