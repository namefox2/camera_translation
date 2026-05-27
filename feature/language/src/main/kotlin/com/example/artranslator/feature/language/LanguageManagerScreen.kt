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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artranslator.core.translation.model.DownloadState

@Composable
fun LanguageManagerScreen(
    viewModel: LanguageManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingDownloadCode by viewModel.pendingDownloadCode.collectAsState()
    val snackbarError by viewModel.snackbarError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // 에러 → Snackbar (WiFi 미연결, 인터넷 없음, 다운로드 실패 모두 포함)
    LaunchedEffect(snackbarError) {
        val msg = snackbarError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Long)
        viewModel.clearSnackbarError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // 전체를 하나의 LazyColumn으로 — 다운된 언어가 많아도 스크롤 가능
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── 안내 배너 ─────────────────────────────────────────────────────
            item { InfoBanner() }

            // ── 다운로드된 언어 ───────────────────────────────────────────────
            if (uiState.downloadedLanguages.isNotEmpty()) {
                item {
                    Text(
                        "다운로드된 언어",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
                items(uiState.downloadedLanguages, key = { "dl_${it.code}" }) { lang ->
                    DownloadedLanguageItem(
                        language = lang,
                        onDelete = { viewModel.deleteLanguage(lang.code) }
                    )
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            // ── 다운로드 가능한 언어 ──────────────────────────────────────────
            item {
                Text(
                    "다운로드 가능한 언어",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            items(uiState.availableLanguages, key = { "avail_${it.code}" }) { lang ->
                AvailableLanguageItem(
                    language = lang,
                    downloadState = uiState.downloadStates[lang.code] ?: DownloadState.NotDownloaded,
                    isDownloaded = uiState.downloadedLanguages.any { it.code == lang.code },
                    onDownload = { viewModel.requestDownload(lang.code) }
                )
            }
        }
    }

    // ── 다운로드 방식 선택 다이얼로그 ─────────────────────────────────────────
    if (pendingDownloadCode != null) {
        val lang = LanguageManagerViewModel.ALL_LANGUAGES.find { it.code == pendingDownloadCode }
        DownloadChoiceDialog(
            languageName = lang?.nativeName ?: pendingDownloadCode ?: "",
            sizeMb = lang?.estimatedSizeMb ?: 0,
            onDownloadNow = { viewModel.confirmDownload(requireWifi = false) },
            onDownloadWifi = { viewModel.confirmDownload(requireWifi = true) },
            onDismiss = { viewModel.cancelDownload() }
        )
    }
}

// ─── 안내 배너 ────────────────────────────────────────────────────────────────

@Composable
private fun InfoBanner() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "💡 Google ML Kit 서버에서 다운로드 (영어 80 MB ~ 중국어 200 MB)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ─── 다운로드 방식 선택 다이얼로그 ────────────────────────────────────────────

@Composable
private fun DownloadChoiceDialog(
    languageName: String,
    sizeMb: Int,
    onDownloadNow: () -> Unit,
    onDownloadWifi: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Download, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("언어 다운로드") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$languageName (~${sizeMb} MB)",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "데이터 용량이 크니 와이파이로 다운로드하시겠습니까?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onDownloadWifi, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("와이파이로 다운")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDownloadNow, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.SignalCellularAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("그냥 다운")
            }
        }
    )
}

// ─── 다운로드된 언어 아이템 ────────────────────────────────────────────────────

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

// ─── 다운로드 가능한 언어 아이템 ──────────────────────────────────────────────

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
                Text(language.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(modifier = Modifier.weight(1f).height(4.dp))
                            Spacer(Modifier.width(8.dp))
                            val elapsed = downloadState.elapsedSeconds
                            Text(
                                if (elapsed > 0) "${elapsed}초 경과…" else "다운로드 중…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is DownloadState.Error -> {
                        Spacer(Modifier.height(4.dp))
                        // 에러 요약만 표시 (전체 에러는 Snackbar로)
                        Text("다운로드 실패 — 다시 시도하려면 ↻ 탭",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    else -> {}
                }
            }
        },
        trailingContent = {
            when {
                isDownloaded -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "다운로드됨",
                    tint = MaterialTheme.colorScheme.secondary
                )
                downloadState is DownloadState.Downloading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                )
                downloadState is DownloadState.Error -> {
                    // 에러 상태: 재시도 버튼
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Refresh, contentDescription = "재시도",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
                else -> IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "다운로드")
                }
            }
        }
    )
}
