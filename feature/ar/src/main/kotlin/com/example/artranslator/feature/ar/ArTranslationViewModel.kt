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
    val sourceScript: TextAnalyzer.Script = TextAnalyzer.Script.LATIN,
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
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            val translated = blocks.mapNotNull { block ->
                if (block.text.isBlank()) return@mapNotNull null
                val result = translationRepository.translate(
                    text = block.text,
                    targetLanguage = _uiState.value.targetLanguage
                )
                when (result) {
                    is TranslationResult.Success -> {
                        val box = block.boundingBox ?: return@mapNotNull null
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
