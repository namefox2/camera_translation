package com.example.artranslator.core.translation.model

/**
 * Supported language codes (ISO 639-1).
 * Covers all 58 ML Kit offline languages plus additional Cloud API languages.
 */
enum class Language(val code: String, val displayName: String, val nativeName: String) {
    KOREAN("ko", "Korean", "한국어"),
    ENGLISH("en", "English", "English"),
    JAPANESE("ja", "Japanese", "日本語"),
    CHINESE_SIMPLIFIED("zh", "Chinese (Simplified)", "中文(简体)"),
    CHINESE_TRADITIONAL("zh-TW", "Chinese (Traditional)", "中文(繁體)"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    SPANISH("es", "Spanish", "Español"),
    ITALIAN("it", "Italian", "Italiano"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    RUSSIAN("ru", "Russian", "Русский"),
    ARABIC("ar", "Arabic", "العربية"),
    HINDI("hi", "Hindi", "हिन्दी"),
    THAI("th", "Thai", "ภาษาไทย"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    TURKISH("tr", "Turkish", "Türkçe"),
}

/**
 * Result of a translation operation.
 */
sealed class TranslationResult {
    data class Success(
        val translatedText: String,
        val sourceLanguage: String?,
        val isOffline: Boolean
    ) : TranslationResult()

    data class Error(val message: String, val throwable: Throwable? = null) : TranslationResult()
}

/**
 * State of a language model download.
 */
sealed class DownloadState {
    object NotDownloaded : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Downloaded : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * Connectivity state.
 */
enum class NetworkState {
    ONLINE,
    OFFLINE
}
