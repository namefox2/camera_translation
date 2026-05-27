package com.example.artranslator.feature.language

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artranslator.core.translation.model.DownloadState

/**
 * Language download management screen.
 * Users can browse available languages, download offline models, and delete them.
 */
@Composable
fun LanguageManagerScreen(
    viewModel: LanguageManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Downloaded languages header
        if (uiState.downloadedLanguages.isNotEmpty()) {
            Text(
                "다운로드된 언어",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
            uiState.downloadedLanguages.forEach { lang ->
                DownloadedLanguageItem(
                    language = lang,
                    onDelete = { viewModel.deleteLanguage(lang.code) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Text(
            "다운로드 가능한 언어",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(uiState.availableLanguages) { lang ->
                AvailableLanguageItem(
                    language = lang,
                    downloadState = uiState.downloadStates[lang.code] ?: DownloadState.NotDownloaded,
                    isDownloaded = uiState.downloadedLanguages.any { it.code == lang.code },
                    onDownload = { viewModel.downloadLanguage(lang.code) }
                )
            }
        }
    }
}

@Composable
private fun DownloadedLanguageItem(
    language: LanguageManagerViewModel.LanguageListItem,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(language.nativeName) },
        supportingContent = { Text(language.displayName) },
        leadingContent = {
            Icon(Icons.Default.CheckCircle, contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary)
        },
        trailingContent = {
            var showConfirm by remember { mutableStateOf(false) }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.error)
            }
            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("언어 삭제") },
                    text = { Text("${language.nativeName} 오프라인 모델을 삭제하시겠습니까?") },
                    confirmButton = {
                        TextButton(onClick = { onDelete(); showConfirm = false }) {
                            Text("삭제", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false }) { Text("취소") }
                    }
                )
            }
        }
    )
}

@Composable
private fun AvailableLanguageItem(
    language: LanguageManagerViewModel.LanguageListItem,
    downloadState: DownloadState,
    isDownloaded: Boolean,
    onDownload: () -> Unit
) {
    ListItem(
        headlineContent = { Text(language.nativeName) },
        supportingContent = {
            Column {
                Text(language.displayName)
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        )
                    }
                    is DownloadState.Error -> Text(
                        downloadState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                    else -> {}
                }
            }
        },
        trailingContent = {
            when {
                isDownloaded -> Icon(Icons.Default.CheckCircle, contentDescription = "다운로드됨",
                    tint = MaterialTheme.colorScheme.secondary)
                downloadState is DownloadState.Downloading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else -> IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "다운로드")
                }
            }
        }
    )
}
