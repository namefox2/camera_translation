package com.example.artranslator.feature.phrasebook

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artranslator.core.database.dao.PhraseDao
import com.example.artranslator.core.database.entity.DownloadedLanguageEntity
import com.example.artranslator.core.database.entity.PhraseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// ─── Models ───────────────────────────────────────────────────────────────────

data class PhraseItem(
    val id: Long,
    val originalText: String,
    val translatedText: String,
    val pronunciation: String,
    val languageCode: String
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
    val phrases: List<PhraseItem> = emptyList()
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class PhrasebookViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phraseDao: PhraseDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhrasebookUiState())
    val uiState: StateFlow<PhrasebookUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null

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
                val categories = rawCategories.map { PhraseCategory(it, it.toCategoryDisplayName()) }
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

    fun speakPhrase(phrase: PhraseItem) {
        val locale = Locale.forLanguageTag(phrase.languageCode)
        tts?.language = locale
        tts?.speak(phrase.translatedText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private fun DownloadedLanguageEntity.toLanguageItem() = LanguageItem(languageCode, displayName, nativeName)
    private fun PhraseEntity.toPhraseItem() = PhraseItem(id, originalText, translatedText, pronunciation, languageCode)
    private fun String.toCategoryDisplayName() = when (this) {
        "greeting" -> "기본 인사"
        "restaurant" -> "쇼핑/식당"
        "accommodation" -> "숙박"
        "transport" -> "교통"
        "emergency" -> "응급"
        "numbers" -> "숫자"
        "meeting" -> "미팅/비즈니스"
        "campus" -> "캠퍼스/학교"
        else -> this
    }
}
