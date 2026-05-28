package com.example.artranslator.feature.phrasebook

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artranslator.core.database.dao.PhraseDao
import com.example.artranslator.core.database.entity.DownloadedLanguageEntity
import com.example.artranslator.core.database.entity.PhraseEntity
import com.example.artranslator.core.translation.TranslationRepository
import com.example.artranslator.core.translation.model.TranslationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale
import javax.inject.Inject

// ─── Models ───────────────────────────────────────────────────────────────────

data class PhraseItem(
    val id: Long,
    val originalText: String,
    val translatedText: String,
    val pronunciation: String,
    val languageCode: String,
    val isCustom: Boolean = false
)

data class PhraseCategory(
    val id: String,
    val displayName: String
)

data class LanguageItem(
    val code: String,
    val displayName: String,
    val nativeName: String
)

data class PhrasebookUiState(
    val downloadedLanguages: List<LanguageItem> = emptyList(),
    val selectedLanguageIndex: Int = 0,
    val categories: List<PhraseCategory> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val phrases: List<PhraseItem> = emptyList(),
    val showAddDialog: Boolean = false,
    val isAutoTranslating: Boolean = false,
    val autoTranslatedText: String = ""
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class PhrasebookViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phraseDao: PhraseDao,
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhrasebookUiState())
    val uiState: StateFlow<PhrasebookUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var autoTranslateJob: Job? = null

    init {
        tts = TextToSpeech(context) { }
        loadDownloadedLanguages()
    }

    private fun loadDownloadedLanguages() {
        viewModelScope.launch {
            phraseDao.getDownloadedLanguages().collect { entities ->
                val languages = entities.map { it.toLanguageItem() }
                _uiState.update { it.copy(downloadedLanguages = languages) }
                if (languages.isNotEmpty()) {
                    loadCategories(languages[_uiState.value.selectedLanguageIndex].code)
                }
            }
        }
    }

    private fun loadCategories(languageCode: String) {
        viewModelScope.launch {
            phraseDao.getCategoriesForLanguage(languageCode).collect { rawCategories ->
                val categories = rawCategories
                    .sortedBy { categoryOrder.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
                    .map { PhraseCategory(it, it.toCategoryDisplayName()) }
                _uiState.update { it.copy(categories = categories, selectedCategoryIndex = 0) }
                if (categories.isNotEmpty()) {
                    loadPhrases(languageCode, categories[0].id)
                }
            }
        }
    }

    private fun loadPhrases(languageCode: String, category: String) {
        viewModelScope.launch {
            phraseDao.getPhrasesByCategory(languageCode, category).collect { entities ->
                _uiState.update { it.copy(phrases = entities.map { e -> e.toPhraseItem() }) }
            }
        }
    }

    fun selectLanguage(index: Int) {
        val lang = _uiState.value.downloadedLanguages.getOrNull(index) ?: return
        _uiState.update { it.copy(selectedLanguageIndex = index) }
        loadCategories(lang.code)
    }

    fun selectCategory(index: Int) {
        val category = _uiState.value.categories.getOrNull(index) ?: return
        val lang = _uiState.value.downloadedLanguages
            .getOrNull(_uiState.value.selectedLanguageIndex) ?: return
        _uiState.update { it.copy(selectedCategoryIndex = index) }
        loadPhrases(lang.code, category.id)
    }

    fun openAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, autoTranslatedText = "", isAutoTranslating = false) }
    }

    fun closeAddDialog() {
        autoTranslateJob?.cancel()
        _uiState.update { it.copy(showAddDialog = false, autoTranslatedText = "", isAutoTranslating = false) }
    }

    fun onOriginalTextChanged(text: String) {
        autoTranslateJob?.cancel()
        if (text.length < 2) {
            _uiState.update { it.copy(autoTranslatedText = "", isAutoTranslating = false) }
            return
        }
        val lang = _uiState.value.downloadedLanguages
            .getOrNull(_uiState.value.selectedLanguageIndex) ?: return

        _uiState.update { it.copy(isAutoTranslating = true) }
        autoTranslateJob = viewModelScope.launch {
            delay(500)
            val result = translationRepository.translate(text, lang.code, "ko")
            _uiState.update { state ->
                when (result) {
                    is TranslationResult.Success -> state.copy(
                        autoTranslatedText = result.translatedText,
                        isAutoTranslating = false
                    )
                    else -> state.copy(isAutoTranslating = false)
                }
            }
        }
    }

    fun addPhrase(originalText: String, translatedText: String, pronunciation: String) {
        val lang = _uiState.value.downloadedLanguages
            .getOrNull(_uiState.value.selectedLanguageIndex) ?: return
        viewModelScope.launch {
            phraseDao.insertPhrase(
                PhraseEntity(
                    languageCode = lang.code,
                    category = "user_custom",
                    originalText = originalText.trim(),
                    translatedText = translatedText.trim(),
                    pronunciation = pronunciation.trim(),
                    sortOrder = System.currentTimeMillis().toInt()
                )
            )
            closeAddDialog()
        }
    }

    fun deletePhrase(phrase: PhraseItem) {
        viewModelScope.launch { phraseDao.deletePhraseById(phrase.id) }
    }

    fun speakPhrase(phrase: PhraseItem) {
        val locale = Locale.forLanguageTag(phrase.languageCode)
        tts?.language = locale
        tts?.speak(phrase.translatedText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCleared() {
        super.onCleared()
        autoTranslateJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private fun DownloadedLanguageEntity.toLanguageItem() = LanguageItem(languageCode, displayName, nativeName)
    private fun PhraseEntity.toPhraseItem() = PhraseItem(
        id, originalText, translatedText, pronunciation, languageCode,
        isCustom = (category == "user_custom")
    )
    private fun String.toCategoryDisplayName() = when (this) {
        "greeting"      -> "기본 인사"
        "restaurant"    -> "쇼핑/식당"
        "accommodation" -> "숙박"
        "transport"     -> "교통"
        "emergency"     -> "응급"
        "numbers"       -> "숫자"
        "meeting"       -> "미팅/비즈니스"
        "campus"        -> "캠퍼스/학교"
        "user_custom"   -> "나만의 문장"
        else            -> this
    }

    companion object {
        private val categoryOrder = listOf(
            "greeting", "restaurant", "accommodation", "transport",
            "emergency", "numbers", "meeting", "campus", "user_custom"
        )
    }
}
