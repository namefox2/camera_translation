package com.example.artranslator.feature.ar

import android.graphics.RectF
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
    val sourceLanguage: String? = null, // null = auto-detect
    val translatedBlocks: List<OverlayView.TranslatedBlock> = emptyList(),
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

    // Pending translation job — cancelled on each new batch
    private var translationJob: Job? = null

    // Frame dimensions received from TextAnalyzer
    private var frameWidth: Int = 1
    private var frameHeight: Int = 1

    /**
     * Called by the camera pipeline each time new text blocks are detected.
     * Cancels any in-flight translation and starts a fresh coroutine.
     */
    fun onTextBlocksDetected(
        blocks: List<TextAnalyzer.TextBlock>,
        frameW: Int,
        frameH: Int
    ) {
        frameWidth = frameW
        frameHeight = frameH
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            val translatedBlocks = blocks.mapNotNull { block ->
                if (block.text.isBlank()) return@mapNotNull null
                val result = translationRepository.translate(
                    text = block.text,
                    targetLanguage = _uiState.value.targetLanguage,
                    sourceLanguage = _uiState.value.sourceLanguage
                )
                when (result) {
                    is TranslationResult.Success -> {
                        val box = block.boundingBox ?: return@mapNotNull null
                        OverlayView.TranslatedBlock(
                            originalText = block.text,
                            translatedText = result.translatedText,
                            boundingBox = RectF(box),
                            normRect = RectF(
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
            _uiState.update { it.copy(translatedBlocks = translatedBlocks, isOnline = true) }
        }
    }

    fun setTargetLanguage(languageCode: String) {
        _uiState.update { it.copy(targetLanguage = languageCode) }
    }

    fun setSourceLanguage(languageCode: String?) {
        _uiState.update { it.copy(sourceLanguage = languageCode) }
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isCameraPermissionGranted = granted) }
    }

    fun clearOverlay() {
        _uiState.update { it.copy(translatedBlocks = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        translationJob?.cancel()
    }
}
