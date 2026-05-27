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

    /** Snackbar 에러 메시지 — WiFi 미연결·인터넷 없음·다운로드 실패 등 모든 에러 */
    private val _snackbarError = MutableStateFlow<String?>(null)
    val snackbarError: StateFlow<String?> = _snackbarError.asStateFlow()

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

    /** 다운로드 버튼 클릭 시 → 다이얼로그 표시 */
    fun requestDownload(languageCode: String) {
        _pendingDownloadCode.value = languageCode
    }

    /**
     * 다이얼로그에서 선택 확정.
     * - requireWifi = false → 즉시 다운로드 (데이터/WiFi 모두 허용)
     * - requireWifi = true  → WiFi 연결 확인 후 다운로드
     */
    fun confirmDownload(requireWifi: Boolean) {
        val code = _pendingDownloadCode.value ?: return
        _pendingDownloadCode.value = null

        // ① WiFi 선택했는데 WiFi 없음
        if (requireWifi && !isWifiConnected()) {
            _snackbarError.value = "📶 와이파이에 연결되어 있지 않습니다.\n와이파이 연결 후 다시 시도해 주세요."
            return
        }

        // ② 인터넷 자체가 없음
        if (!isInternetConnected()) {
            _snackbarError.value = "🌐 인터넷 연결을 확인해 주세요.\n언어팩은 Google 서버에서 다운로드됩니다 (80~200 MB)."
            return
        }

        startDownload(code, requireWifi)
    }

    /** 다이얼로그 취소 */
    fun cancelDownload() {
        _pendingDownloadCode.value = null
    }

    /** Snackbar 표시 후 에러 초기화 */
    fun clearSnackbarError() {
        _snackbarError.value = null
    }

    private fun startDownload(languageCode: String, requireWifi: Boolean) {
        viewModelScope.launch {
            translationRepository.downloadLanguageModel(languageCode, requireWifi).collect { state ->
                _uiState.update {
                    it.copy(downloadStates = it.downloadStates + (languageCode to state))
                }
                when (state) {
                    is DownloadState.Downloaded -> {
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
                    is DownloadState.Error -> {
                        // 에러를 Snackbar로도 표시 (목록 아이템의 작은 텍스트만으로 부족)
                        _snackbarError.value = state.message
                    }
                    else -> {}
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

    // ─── 네트워크 상태 확인 ───────────────────────────────────────────────────────

    private fun isInternetConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
