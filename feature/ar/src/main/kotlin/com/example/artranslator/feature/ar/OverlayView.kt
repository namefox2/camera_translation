package com.example.artranslator.feature.ar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * 카메라 프리뷰 위에 번역 결과를 원본 텍스트 위치에 덮어씌우는 뷰.
 *
 * - PreviewView.ScaleType.FILL_CENTER 좌표 변환 적용
 * - 원본 텍스트 영역을 배경으로 덮고, 번역 텍스트를 그 위에 표시
 * - 텍스트 크기 자동 조정 (영역에 맞게)
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class TranslatedBlock(
        val originalText: String,
        val translatedText: String,
        /** 정규화 좌표 [0..1] — frameWidth/Height 기준 */
        val normRect: RectF
    )

    // ─── Paint (onDraw 내에서 생성 금지) ──────────────────────────────────────

    /** 원본 텍스트를 덮는 배경 */
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xCC534AB7.toInt()  // 기본 테마색 (setOverlayColor로 변경)
    }

    /** 배경 테두리 */
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0x88FFFFFF.toInt()
    }

    /** 번역 텍스트 */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = 36f
        setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }

    private val textBounds = Rect()

    // ─── 상태 ─────────────────────────────────────────────────────────────────

    @Volatile private var blocks: List<TranslatedBlock> = emptyList()
    private var frameWidth: Int = 1
    private var frameHeight: Int = 1

    // ─── 공개 API ──────────────────────────────────────────────────────────────

    fun updateBlocks(newBlocks: List<TranslatedBlock>, frameW: Int = frameWidth, frameH: Int = frameHeight) {
        blocks = newBlocks
        frameWidth = frameW
        frameHeight = frameH
        post { invalidate() }
    }

    fun setOverlayColor(color: Int) {
        bgPaint.color = (color and 0x00FFFFFF) or 0xCC000000.toInt()  // 알파 0xCC 고정
        post { invalidate() }
    }

    fun clearOverlay() {
        blocks = emptyList()
        post { invalidate() }
    }

    // ─── 그리기 ────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentBlocks = blocks
        if (currentBlocks.isEmpty() || width == 0 || height == 0) return

        // ── FILL_CENTER 좌표 변환 ─────────────────────────────────────────────
        // PreviewView가 FILL_CENTER 모드이므로 동일한 변환 적용
        val scaleX = width.toFloat() / frameWidth
        val scaleY = height.toFloat() / frameHeight
        val scale = maxOf(scaleX, scaleY)
        val displayedW = frameWidth * scale
        val displayedH = frameHeight * scale
        val offsetX = (width - displayedW) / 2f   // 음수 = 좌우가 잘림
        val offsetY = (height - displayedH) / 2f  // 음수 = 위아래가 잘림

        for (block in currentBlocks) {
            // normRect → 뷰 좌표
            val vLeft   = block.normRect.left   * displayedW + offsetX
            val vTop    = block.normRect.top    * displayedH + offsetY
            val vRight  = block.normRect.right  * displayedW + offsetX
            val vBottom = block.normRect.bottom * displayedH + offsetY

            val viewRect = RectF(vLeft, vTop, vRight, vBottom)

            // 화면 밖 블록 스킵
            if (!viewRect.intersect(0f, 0f, width.toFloat(), height.toFloat())) continue
            if (viewRect.width() < 6f || viewRect.height() < 6f) continue

            val displayText = block.translatedText.ifBlank { block.originalText }

            // ── 1. 원본 텍스트 영역을 배경으로 덮기 ──────────────────────────
            canvas.drawRoundRect(viewRect, 6f, 6f, bgPaint)
            canvas.drawRoundRect(viewRect, 6f, 6f, strokePaint)

            // ── 2. 텍스트 크기 자동 조정 ─────────────────────────────────────
            val maxFontH = viewRect.height() * 0.65f
            textPaint.textSize = maxFontH.coerceIn(13f, 44f)

            // 폭이 넘치면 줄임
            val measuredW = textPaint.measureText(displayText)
            val availW = viewRect.width() - 8f
            if (measuredW > availW && measuredW > 0f) {
                textPaint.textSize *= (availW / measuredW)
                textPaint.textSize = textPaint.textSize.coerceAtLeast(11f)
            }

            // ── 3. 번역 텍스트를 영역 중앙에 그리기 ─────────────────────────
            textPaint.getTextBounds(displayText, 0, displayText.length, textBounds)
            val textX = viewRect.left + (viewRect.width() - textPaint.measureText(displayText)) / 2f
            val textY = viewRect.centerY() + textBounds.height() / 2f - textPaint.descent()
            canvas.drawText(displayText, textX, textY, textPaint)
        }
    }
}
