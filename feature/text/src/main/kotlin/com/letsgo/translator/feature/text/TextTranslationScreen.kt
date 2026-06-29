package com.letsgo.translator.feature.text

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTranslationScreen(
    viewModel: TextTranslationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Language Selector Row
        LanguageSelector(
            sourceLanguage = uiState.sourceLanguage,
            targetLanguage = uiState.targetLanguage,
            onSourceChanged = viewModel::setSourceLanguage,
            onTargetChanged = viewModel::setTargetLanguage,
            onSwap = viewModel::swapLanguages
        )

        Spacer(Modifier.height(16.dp))

        // Source text input
        OutlinedTextField(
            value = uiState.inputText,
            onValueChange = viewModel::setInputText,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = { Text("번역할 텍스트를 입력하세요") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                viewModel.translate()
            }),
            trailingIcon = {
                if (uiState.inputText.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearInput) {
                        Icon(Icons.Default.Clear, contentDescription = "지우기")
                    }
                }
            },
            label = { Text("원문") }
        )

        Spacer(Modifier.height(8.dp))

        // Translate button
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.translate()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.inputText.isNotEmpty() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("번역하기")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Translation result
        if (uiState.translatedText.isNotEmpty()) {
            TranslationResultCard(
                translatedText = uiState.translatedText,
                detectedLanguage = uiState.detectedLanguage,
                isOffline = uiState.isOffline,
                onCopy = viewModel::copyToClipboard,
                onSpeak = viewModel::speakTranslation
            )
        }

        // Error
        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    sourceLanguage: String,
    targetLanguage: String,
    onSourceChanged: (String) -> Unit,
    onTargetChanged: (String) -> Unit,
    onSwap: () -> Unit
) {
    val languages = listOf(
        "auto" to "자동 감지",
        "ko" to "한국어",
        "en" to "영어",
        "ja" to "일본어",
        "zh" to "중국어(간체)",
        "fr" to "프랑스어",
        "de" to "독일어",
        "es" to "스페인어",
        "it" to "이탈리아어",
        "pt" to "포르투갈어",
        "ru" to "러시아어",
        "ar" to "아랍어",
        "hi" to "힌디어",
        "th" to "태국어",
        "vi" to "베트남어",
        "id" to "인도네시아어",
        "tr" to "터키어"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageDropdown(
            selected = sourceLanguage,
            languages = languages,
            onSelected = onSourceChanged,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onSwap) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "언어 교환")
        }

        LanguageDropdown(
            selected = targetLanguage,
            languages = languages.filter { it.first != "auto" },
            onSelected = onTargetChanged,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LanguageDropdown(
    selected: String,
    languages: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = languages.find { it.first == selected }?.second ?: selected

    Box(modifier = modifier) {
        FilledTonalButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedName, maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TranslationResultCard(
    translatedText: String,
    detectedLanguage: String?,
    isOffline: Boolean,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("번역 결과", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Row {
                    if (isOffline) {
                        AssistChip(
                            onClick = {},
                            label = { Text("오프라인", style = MaterialTheme.typography.labelSmall) }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = onSpeak, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "발음 듣기",
                            modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "복사",
                            modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(translatedText, style = MaterialTheme.typography.bodyLarge)

            detectedLanguage?.let {
                Spacer(Modifier.height(4.dp))
                Text("감지된 언어: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
