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
    val isCameraPermissionGranted: Boolean = false
)

@HiltViewModel
class ArTranslationViewModel @Inject constructor(
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    private var translationJob: Job? = null

    fun onTextBlocksDetected(
        blocks: List<TextAnalyzer.TextBlock>,
        frameW: Int,
        frameH: Int
    ) {
        if (translationJob?.isActive == true) return
        if (frameW <= 0 || frameH <= 0) return

        translationJob = viewModelScope.launch {
            val targetLang = _uiState.value.targetLanguage
            var anyOnline = false

            val translated = supervisorScope {
                blocks
                    .filter { it.text.isNotBlank() && it.boundingBox != null }
                    .map { block ->
                        async {
                            val box = block.boundingBox!!
                            val result = translationRepository.translate(
                                text = block.text,
                                targetLanguage = targetLang
                            )
                            when (result) {
                                is TranslationResult.Success -> {
                                    if (!result.isOffline) anyOnline = true
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
                                is TranslationResult.Error -> null
                            }
                        }
                    }.awaitAll().filterNotNull()
            }

            _uiState.update { it.copy(
                translatedBlocks = translated,
                frameWidth = frameW,
                frameHeight = frameH,
                isOnline = if (translated.isEmpty()) it.isOnline else anyOnline
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
