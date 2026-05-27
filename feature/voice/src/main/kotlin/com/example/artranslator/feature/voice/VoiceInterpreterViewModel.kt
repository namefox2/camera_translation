package com.example.artranslator.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artranslator.core.translation.TranslationRepository
import com.example.artranslator.core.translation.model.TranslationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class VoiceUiState(
    val isListening: Boolean = false,
    val isLoading: Boolean = false,
    val recognizedText: String = "",
    val translatedText: String = "",
    val sourceLanguage: String = "ko",
    val targetLanguage: String = "en",
    val errorMessage: String? = null,
    /** Cloud API(온라인) 사용 시 false, ML Kit 폴백(온디바이스) 시 true */
    val isOfflineTranslation: Boolean = false
)

@HiltViewModel
class VoiceInterpreterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { }
    }

    fun startListening() {
        _uiState.update { it.copy(isListening = true, recognizedText = "", errorMessage = null) }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _uiState.update { it.copy(isLoading = true) }
                }
                override fun onError(error: Int) {
                    _uiState.update {
                        it.copy(isListening = false, isLoading = false,
                            errorMessage = "음성 인식 오류 (코드: $error)")
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    _uiState.update { it.copy(recognizedText = text, isListening = false) }
                    if (text.isNotBlank()) translateRecognizedText(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    _uiState.update { it.copy(recognizedText = partial) }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                    _uiState.value.sourceLanguage.toLocaleTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            startListening(intent)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    private fun translateRecognizedText(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = translationRepository.translate(
                text = text,
                targetLanguage = _uiState.value.targetLanguage,
                sourceLanguage = _uiState.value.sourceLanguage
            )
            when (result) {
                is TranslationResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        translatedText = result.translatedText,
                        isOfflineTranslation = result.isOffline
                    ) }
                    speakTranslation()
                }
                is TranslationResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun speakTranslation() {
        val text = _uiState.value.translatedText
        if (text.isBlank()) return
        val locale = Locale.forLanguageTag(_uiState.value.targetLanguage)
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun swapLanguages() {
        _uiState.update {
            it.copy(sourceLanguage = it.targetLanguage, targetLanguage = it.sourceLanguage)
        }
    }

    fun setSourceLanguage(code: String) = _uiState.update { it.copy(sourceLanguage = code) }
    fun setTargetLanguage(code: String) = _uiState.update { it.copy(targetLanguage = code) }

    private fun String.toLocaleTag(): String = when (this) {
        "ko" -> "ko-KR"
        "en" -> "en-US"
        "ja" -> "ja-JP"
        "zh" -> "zh-CN"
        "fr" -> "fr-FR"
        "de" -> "de-DE"
        "es" -> "es-ES"
        "th" -> "th-TH"
        else -> this
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
