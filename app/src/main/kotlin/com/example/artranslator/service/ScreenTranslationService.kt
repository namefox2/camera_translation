package com.example.artranslator.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.example.artranslator.core.translation.TranslationRepository
import com.example.artranslator.core.translation.model.TranslationResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*

class ScreenTranslationService : AccessibilityService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ScreenTranslationEntryPoint {
        fun translationRepository(): TranslationRepository
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var translationJob: Job? = null

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayText: TextView? = null

    private lateinit var translationRepository: TranslationRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        translationRepository = EntryPointAccessors.fromApplication(
            applicationContext,
            ScreenTranslationEntryPoint::class.java
        ).translationRepository()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) return

        val node = event.source ?: run { hideOverlay(); return }
        val fullText = node.text
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        node.recycle()

        if (fullText == null || start < 0 || end <= start || start >= fullText.length) {
            hideOverlay()
            return
        }

        val selected = fullText.substring(
            start.coerceAtLeast(0),
            end.coerceAtMost(fullText.length)
        ).trim()

        if (selected.isBlank()) { hideOverlay(); return }

        showOverlay("번역 중...")

        translationJob?.cancel()
        translationJob = serviceScope.launch {
            val targetLang = getSharedPreferences("screen_translation_prefs", Context.MODE_PRIVATE)
                .getString("target_language", "ko") ?: "ko"
            when (val result = translationRepository.translate(selected, targetLang)) {
                is TranslationResult.Success      -> updateOverlay(result.translatedText)
                is TranslationResult.Error        -> updateOverlay("번역 실패")
                is TranslationResult.QuotaExceeded -> updateOverlay("번역 횟수 초과 — 앱에서 광고 시청 후 재시도")
            }
        }
    }

    override fun onInterrupt() = hideOverlay()

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        serviceScope.cancel()
    }

    // ── Overlay ────────────────────────────────────────────────────────────────

    private fun showOverlay(text: String) {
        if (overlayView == null) buildOverlay()
        overlayText?.text = text
    }

    private fun updateOverlay(text: String) {
        overlayText?.text = text
    }

    private fun hideOverlay() {
        overlayView?.let { v -> try { windowManager?.removeView(v) } catch (_: Exception) {} }
        overlayView = null
        overlayText = null
    }

    private fun buildOverlay() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        // Root card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.argb(242, 255, 255, 255))
            }
            elevation = dp(8).toFloat()
            setPadding(dp(16), dp(12), dp(16), dp(14))
        }

        // Header row — drag handle + close button
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = "화면 번역"
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#6750A4"))
        }

        val closeView = TextView(this).apply {
            text = "  ✕"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setOnClickListener { hideOverlay() }
        }

        header.addView(titleView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(closeView)

        // Translation result text
        val contentView = TextView(this).apply {
            text = "번역 중..."
            textSize = 15f
            setTextColor(Color.BLACK)
            setPadding(0, dp(6), 0, 0)
            maxWidth = dp(280)
        }
        overlayText = contentView

        card.addView(header)
        card.addView(contentView)

        // Window layout params — bottom-center, no focus steal
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(80)
        }

        // Drag via title area (leaves close button untouched)
        var dragRawX = 0f; var dragRawY = 0f
        var dragInitX = 0; var dragInitY = 0
        titleView.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragRawX = ev.rawX; dragRawY = ev.rawY
                    dragInitX = params.x; dragInitY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (dragInitX + (ev.rawX - dragRawX)).toInt()
                    params.y = (dragInitY - (ev.rawY - dragRawY)).toInt()
                    try { windowManager?.updateViewLayout(card, params) } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(card, params)
        overlayView = card
    }
}
