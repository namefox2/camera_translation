package com.example.artranslator.feature.ar

import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artranslator.core.translation.TranslationRepository
import com.example.artranslator.core.translation.model.TranslationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ArUiState(
    val isLoading: Boolean = false,
    val targetLanguage: String = "ko",
    val sourceScript: TextAnalyzer.Script = TextAnalyzer.Script.AUTO,
    val translatedBlocks: List<OverlayView.TranslatedBlock> = emptyList(),
    val frameWidth: Int = 1,
    val frameHeight: Int = 1,
    val errorMessage: String? = null,
    val isOnline: Boolean = true,
    val isCameraPermissionGranted: Boolean = false
)

@HiltViewModel
class ArTranslationViewModel @Inject constructor(
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    private var translationJob: Job? = null

    // "원문|타겟언어" → 번역문 캐시 (200개 LRU)
    // 같은 메뉴판을 계속 보면 API 호출 없이 즉시 오버레이 갱신
    private val translationCache = LruCache<String, String>(200)

    fun onTextBlocksDetected(
        blocks: List<TextAnalyzer.TextBlock>,
        frameW: Int,
        frameH: Int
    ) {
        // 이미 번역 중이면 스킵 — 완료 후 다음 프레임(1초 뒤)에 자동 갱신
        // (이전 방식: cancel() → 항상 취소 → 오버레이 미표시 버그)
        if (translationJob?.isActive == true) return

        translationJob = viewModelScope.launch {
            val targetLang = _uiState.value.targetLanguage

            // 모든 블록을 병렬 번역 (Cloud API 왕복 1회 = 전체 완료)
            val translated = supervisorScope {
                blocks
                    .filter { it.text.isNotBlank() && it.boundingBox != null }
                    .map { block ->
                        async {
                            val box = block.boundingBox!!
                            val cacheKey = "${block.text}|$targetLang"
                            val translatedText = translationCache.get(cacheKey)
                                ?: run {
                                    val result = translationRepository.translate(
                                        text = block.text,
                                        targetLanguage = targetLang
                                    )
                                    when (result) {
                                        is TranslationResult.Success -> {
                                            translationCache.put(cacheKey, result.translatedText)
                                            result.translatedText
                                        }
                                        is TranslationResult.Error -> return@async null
                                    }
                                }
                            OverlayView.TranslatedBlock(
                                originalText = block.text,
                                translatedText = translatedText,
                                normRect = android.graphics.RectF(
                                    box.left.toFloat() / frameW,
                                    box.top.toFloat() / frameH,
                                    box.right.toFloat() / frameW,
                                    box.bottom.toFloat() / frameH
                                )
                            )
                        }
                    }.awaitAll().filterNotNull()
            }

            _uiState.update { it.copy(
                translatedBlocks = translated,
                frameWidth = frameW,
                frameHeight = frameH,
                isOnline = true
            )}
        }
    }

    fun setTargetLanguage(languageCode: String) =
        _uiState.update { it.copy(targetLanguage = languageCode) }

    fun setSourceScript(script: TextAnalyzer.Script) =
        _uiState.update { it.copy(sourceScript = script) }

    fun setCameraPermissionGranted(granted: Boolean) =
        _uiState.update { it.copy(isCameraPermissionGranted = granted) }

    fun clearOverlay() =
        _uiState.update { it.copy(translatedBlocks = emptyList()) }

    override fun onCleared() {
        super.onCleared()
        translationJob?.cancel()
    }
}
