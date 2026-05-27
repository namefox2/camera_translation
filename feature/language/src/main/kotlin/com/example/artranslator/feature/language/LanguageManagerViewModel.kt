package com.example.artranslator.feature.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artranslator.core.database.dao.PhraseDao
import com.example.artranslator.core.database.entity.DownloadedLanguageEntity
import com.example.artranslator.core.translation.TranslationRepository
import com.example.artranslator.core.translation.model.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageManagerViewModel @Inject constructor(
    private val translationRepository: TranslationRepository,
    private val phraseDao: PhraseDao
) : ViewModel() {

    data class LanguageListItem(
        val code: String,
        val displayName: String,
        val nativeName: String,
        val estimatedSizeMb: Int
    )

    data class LanguageManagerUiState(
        val availableLanguages: List<LanguageListItem> = ALL_LANGUAGES,
        val downloadedLanguages: List<LanguageListItem> = emptyList(),
        val downloadStates: Map<String, DownloadState> = emptyMap()
    )

    private val _uiState = MutableStateFlow(LanguageManagerUiState())
    val uiState: StateFlow<LanguageManagerUiState> = _uiState.asStateFlow()

    init {
        observeDownloadedLanguages()
    }

    private fun observeDownloadedLanguages() {
        viewModelScope.launch {
            phraseDao.getDownloadedLanguages().collect { entities ->
                val items = entities.map { it.toListItem() }
                _uiState.update { it.copy(downloadedLanguages = items) }
            }
        }
    }

    fun downloadLanguage(languageCode: String) {
        viewModelScope.launch {
            translationRepository.downloadLanguageModel(languageCode).collect { state ->
                _uiState.update {
                    it.copy(downloadStates = it.downloadStates + (languageCode to state))
                }
                if (state == DownloadState.Downloaded) {
                    val lang = ALL_LANGUAGES.find { l -> l.code == languageCode } ?: return@collect
                    phraseDao.insertDownloadedLanguage(
                        DownloadedLanguageEntity(
                            languageCode = lang.code,
                            displayName = lang.displayName,
                            nativeName = lang.nativeName,
                            modelSizeMb = lang.estimatedSizeMb
                        )
                    )
                }
            }
        }
    }

    fun deleteLanguage(languageCode: String) {
        viewModelScope.launch {
            val success = translationRepository.deleteLanguageModel(languageCode)
            if (success) {
                phraseDao.deleteDownloadedLanguage(languageCode)
                phraseDao.deletePhrasesForLanguage(languageCode)
                _uiState.update {
                    it.copy(downloadStates = it.downloadStates - languageCode)
                }
            }
        }
    }

    private fun DownloadedLanguageEntity.toListItem() = LanguageListItem(
        code = languageCode,
        displayName = displayName,
        nativeName = nativeName,
        estimatedSizeMb = modelSizeMb
    )

    companion object {
        val ALL_LANGUAGES = listOf(
            LanguageListItem("en", "English", "영어", 80),
            LanguageListItem("ja", "Japanese", "日本語", 150),
            LanguageListItem("zh", "Chinese (Simplified)", "中文(简体)", 200),
            LanguageListItem("zh-TW", "Chinese (Traditional)", "中文(繁體)", 200),
            LanguageListItem("fr", "French", "Français", 90),
            LanguageListItem("de", "German", "Deutsch", 90),
            LanguageListItem("es", "Spanish", "Español", 90),
            LanguageListItem("it", "Italian", "Italiano", 85),
            LanguageListItem("pt", "Portuguese", "Português", 85),
            LanguageListItem("ru", "Russian", "Русский", 100),
            LanguageListItem("ar", "Arabic", "العربية", 110),
            LanguageListItem("hi", "Hindi", "हिन्दी", 120),
            LanguageListItem("th", "Thai", "ภาษาไทย", 130),
            LanguageListItem("vi", "Vietnamese", "Tiếng Việt", 95),
            LanguageListItem("id", "Indonesian", "Bahasa Indonesia", 85),
            LanguageListItem("tr", "Turkish", "Türkçe", 85),
        )
    }
}
