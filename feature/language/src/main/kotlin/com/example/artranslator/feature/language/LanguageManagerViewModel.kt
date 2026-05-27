package com.example.artranslator.feature.language

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artranslator.core.database.dao.PhraseDao
import com.example.artranslator.core.database.entity.DownloadedLanguageEntity
import com.example.artranslator.core.translation.TranslationRepository
import com.example.artranslator.core.translation.model.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

    /** 다운로드 방식 선택 다이얼로그를 띄울 언어 코드 (null = 다이얼로그 숨김) */
    private val _pendingDownloadCode = MutableStateFlow<String?>(null)
    val pendingDownloadCode: StateFlow<String?> = _pendingDownloadCode.asStateFlow()

    /** 와이파이 미연결 에러 메시지 (null = 에러 없음) */
    private val _noWifiError = MutableStateFlow<String?>(null)
    val noWifiError: StateFlow<String?> = _noWifiError.asStateFlow()

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

    /**
     * 다운로드 버튼 클릭 시 호출 → 다이얼로그(그냥 다운 / 와이파이로 다운) 표시
     */
    fun requestDownload(languageCode: String) {
        _pendingDownloadCode.value = languageCode
    }

    /**
     * 다이얼로그에서 선택 확정:
     * - requireWifi = false → 데이터/WiFi 상관없이 즉시 다운로드
     * - requireWifi = true  → WiFi 연결 확인 후 다운로드, 미연결 시 에러
     */
    fun confirmDownload(requireWifi: Boolean) {
        val code = _pendingDownloadCode.value ?: return
        _pendingDownloadCode.value = null   // 다이얼로그 닫기

        if (requireWifi && !isWifiConnected()) {
            _noWifiError.value = "와이파이에 연결되어 있지 않습니다.\n와이파이 연결 후 다시 시도해 주세요."
            return
        }

        startDownload(code, requireWifi)
    }

    /** 다이얼로그 취소 */
    fun cancelDownload() {
        _pendingDownloadCode.value = null
    }

    /** Snackbar 표시 후 에러 상태 초기화 */
    fun clearWifiError() {
        _noWifiError.value = null
    }

    private fun startDownload(languageCode: String, requireWifi: Boolean) {
        viewModelScope.launch {
            translationRepository.downloadLanguageModel(languageCode, requireWifi).collect { state ->
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

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
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
