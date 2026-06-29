package com.letsgo.translator.feature.text

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsgo.translator.core.translation.TranslationRepository
import com.letsgo.translator.core.translation.model.TranslationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class TextTranslationUiState(
    val inputText: String = "",
    val translatedText: String = "",
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "ko",
    val detectedLanguage: String? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TextTranslationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextTranslationUiState())
    val uiState: StateFlow<TextTranslationUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.ERROR) {
                tts = null
            }
        }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun setSourceLanguage(code: String) {
        _uiState.update { it.copy(sourceLanguage = code) }
    }

    fun setTargetLanguage(code: String) {
        _uiState.update { it.copy(targetLanguage = code) }
    }

    fun swapLanguages() {
        val state = _uiState.value
        val newSource = if (state.sourceLanguage == "auto") "en" else state.sourceLanguage
        _uiState.update {
            it.copy(
                sourceLanguage = state.targetLanguage,
                targetLanguage = newSource,
                inputText = it.translatedText,
                translatedText = it.inputText
            )
        }
    }

    fun translate() {
        val state = _uiState.value
        if (state.inputText.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = translationRepository.translate(
                text = state.inputText,
                targetLanguage = state.targetLanguage,
                sourceLanguage = if (state.sourceLanguage == "auto") null else state.sourceLanguage
            )
            when (result) {
                is TranslationResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        translatedText = result.translatedText,
                        detectedLanguage = result.sourceLanguage,
                        isOffline = result.isOffline
                    )
                }
                is TranslationResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is TranslationResult.QuotaExceeded -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "오늘 번역 횟수를 모두 사용했습니다. 광고를 시청하면 추가로 사용할 수 있어요.")
                }
            }
        }
    }

    fun clearInput() {
        _uiState.update { it.copy(inputText = "", translatedText = "", errorMessage = null) }
    }

    fun copyToClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("translation", _uiState.value.translatedText))
    }

    fun speakTranslation() {
        val text = _uiState.value.translatedText
        if (text.isBlank()) return
        val locale = when (_uiState.value.targetLanguage) {
            "ko" -> Locale.KOREAN
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            else -> Locale.ENGLISH
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
