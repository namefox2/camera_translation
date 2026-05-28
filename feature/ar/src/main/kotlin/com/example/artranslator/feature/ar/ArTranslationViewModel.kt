package com.example.artranslator.feature.ar

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
    val isCameraPermissionGranted: Boolean = false,
    val isFrozen: Boolean = false
)

@HiltViewModel
class ArTranslationViewModel @Inject constructor(
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    private var translationJob: Job? = null
    private val vmCache = HashMap<String, String>(50)
    private var emptyFrameCount = 0
    private var lastOnlineError: String? = null

    fun toggleFreeze() {
        _uiState.update { it.copy(isFrozen = !it.isFrozen) }
    }

    fun onTextBlocksDetected(
        blocks: List<TextAnalyzer.TextBlock>,
        frameW: Int,
        frameH: Int
    ) {
        if (translationJob?.isActive == true) return
        if (frameW <= 0 || frameH <= 0) return

        val filteredBlocks = blocks.filter { it.text.isNotBlank() && it.boundingBox != null }

        // 텍스트가 없는 프레임이 연속 5회 이상일 때만 오버레이 초기화
        if (filteredBlocks.isEmpty()) {
            emptyFrameCount++
            if (emptyFrameCount >= 5) {
                emptyFrameCount = 0
                _uiState.update { it.copy(translatedBlocks = emptyList()) }
            }
            return
        }
        emptyFrameCount = 0

        translationJob = viewModelScope.launch {
            val targetLang = _uiState.value.targetLanguage
            var anyOnline = false

            // 캐시 히트 블록은 즉시 표시
            val cachedBlocks = filteredBlocks.mapNotNull { block ->
                val box = block.boundingBox!!
                val cached = vmCache["${block.text}|$targetLang"] ?: return@mapNotNull null
                OverlayView.TranslatedBlock(
                    originalText = block.text,
                    translatedText = cached,
                    normRect = android.graphics.RectF(
                        box.left.toFloat() / frameW,
                        box.top.toFloat() / frameH,
                        box.right.toFloat() / frameW,
                        box.bottom.toFloat() / frameH
                    )
                )
            }
            if (cachedBlocks.isNotEmpty()) {
                _uiState.update { it.copy(
                    translatedBlocks = cachedBlocks,
                    frameWidth = frameW,
                    frameHeight = frameH
                )}
            }

            // API 번역이 필요한 블록만 비동기 처리
            val needsApi = filteredBlocks.filter { vmCache["${it.text}|$targetLang"] == null }
            if (needsApi.isEmpty()) return@launch

            val apiResults = supervisorScope {
                needsApi.map { block ->
                    async {
                        val box = block.boundingBox!!
                        val result = translationRepository.translate(
                            text = block.text,
                            targetLanguage = targetLang
                        )
                        when (result) {
                            is TranslationResult.Success -> {
                                if (!result.isOffline) {
                                    anyOnline = true
                                    lastOnlineError = null
                                    if (vmCache.size >= 100) vmCache.clear()
                                    vmCache["${block.text}|$targetLang"] = result.translatedText
                                } else if (result.onlineError != null && lastOnlineError == null) {
                                    lastOnlineError = result.onlineError
                                }
                                OverlayView.TranslatedBlock(
                                    originalText = block.text,
                                    translatedText = result.translatedText,
                                    normRect = android.graphics.RectF(
                                        box.left.toFloat() / frameW,
                                        box.top.toFloat() / frameH,
                                        box.right.toFloat() / frameW,
                                        box.bottom.toFloat() / frameH
                                    )
                                )
                            }
                            else -> null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            if (apiResults.isNotEmpty()) {
                val allBlocks = (cachedBlocks + apiResults)
                    .distinctBy { it.originalText }
                _uiState.update { it.copy(
                    translatedBlocks = allBlocks,
                    frameWidth = frameW,
                    frameHeight = frameH,
                    isOnline = anyOnline,
                    errorMessage = if (anyOnline) null else lastOnlineError
                )}
            }
        }
    }

    fun setTargetLanguage(languageCode: String) =
        _uiState.update { it.copy(targetLanguage = languageCode) }

    fun setSourceScript(script: TextAnalyzer.Script) =
        _uiState.update { it.copy(sourceScript = script) }

    fun swapLanguages() {
        val state = _uiState.value
        val newTarget = state.sourceScript.toLanguageCode() ?: return
        val newScript = state.targetLanguage.toScript()
        _uiState.update { it.copy(sourceScript = newScript, targetLanguage = newTarget) }
    }

    private fun TextAnalyzer.Script.toLanguageCode(): String? = when (this) {
        TextAnalyzer.Script.JAPANESE -> "ja"
        TextAnalyzer.Script.CHINESE  -> "zh"
        TextAnalyzer.Script.KOREAN   -> "ko"
        TextAnalyzer.Script.LATIN    -> "en"
        TextAnalyzer.Script.AUTO     -> null
    }

    private fun String.toScript(): TextAnalyzer.Script = when (this) {
        "ja" -> TextAnalyzer.Script.JAPANESE
        "zh" -> TextAnalyzer.Script.CHINESE
        "ko" -> TextAnalyzer.Script.KOREAN
        else -> TextAnalyzer.Script.LATIN
    }

    fun setCameraPermissionGranted(granted: Boolean) =
        _uiState.update { it.copy(isCameraPermissionGranted = granted) }

    fun clearOverlay() =
        _uiState.update { it.copy(translatedBlocks = emptyList()) }

    override fun onCleared() {
        super.onCleared()
        translationJob?.cancel()
    }
}
