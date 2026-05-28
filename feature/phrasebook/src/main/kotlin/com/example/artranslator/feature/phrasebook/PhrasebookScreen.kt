package com.example.artranslator.feature.phrasebook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhrasebookScreen(
    contextTabLabel: String = "회화",
    viewModel: PhrasebookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.downloadedLanguages.isEmpty()) {
        EmptyPhrasebookState()
        return
    }

    val currentLang = uiState.downloadedLanguages.getOrNull(uiState.selectedLanguageIndex)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openAddDialog) {
                Icon(Icons.Default.Add, contentDescription = "문장 추가")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {

            // Language tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedLanguageIndex,
                edgePadding = 0.dp
            ) {
                uiState.downloadedLanguages.forEachIndexed { index, lang ->
                    Tab(
                        selected = uiState.selectedLanguageIndex == index,
                        onClick = { viewModel.selectLanguage(index) },
                        text = { Text(lang.nativeName) }
                    )
                }
            }

            // Category tabs
            val categories = uiState.categories
            if (categories.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedCategoryIndex,
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = uiState.selectedCategoryIndex == index,
                            onClick = { viewModel.selectCategory(index) },
                            text = { Text(category.displayName) }
                        )
                    }
                }
            }

            // Phrases list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.phrases, key = { it.id }) { phrase ->
                    PhraseCard(
                        phrase = phrase,
                        onSpeak = { viewModel.speakPhrase(phrase) },
                        onDelete = if (phrase.isCustom) ({ viewModel.deletePhrase(phrase) }) else null
                    )
                }
            }
        }
    }

    // 문장 추가 다이얼로그
    if (uiState.showAddDialog) {
        AddPhraseDialog(
            targetLanguageName = currentLang?.nativeName ?: "",
            onConfirm = { original, translated, pronunciation ->
                viewModel.addPhrase(original, translated, pronunciation)
            },
            onDismiss = viewModel::closeAddDialog
        )
    }
}

@Composable
private fun AddPhraseDialog(
    targetLanguageName: String,
    onConfirm: (original: String, translated: String, pronunciation: String) -> Unit,
    onDismiss: () -> Unit
) {
    var original by remember { mutableStateOf("") }
    var translated by remember { mutableStateOf("") }
    var pronunciation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("문장 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = original,
                    onValueChange = { original = it },
                    label = { Text("한국어 (원문)") },
                    placeholder = { Text("예: 화장실이 어디예요?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = translated,
                    onValueChange = { translated = it },
                    label = { Text("번역 ($targetLanguageName)") },
                    placeholder = { Text("번역된 문장") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pronunciation,
                    onValueChange = { pronunciation = it },
                    label = { Text("발음 (선택)") },
                    placeholder = { Text("발음 표기") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(original, translated, pronunciation) },
                enabled = original.isNotBlank() && translated.isNotBlank()
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
private fun PhraseCard(
    phrase: PhraseItem,
    onSpeak: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(phrase.originalText, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text(phrase.translatedText, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
                if (phrase.pronunciation.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text("[${phrase.pronunciation}]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            IconButton(onClick = onSpeak) {
                Icon(Icons.Default.VolumeUp, contentDescription = "발음 듣기",
                    tint = MaterialTheme.colorScheme.primary)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EmptyPhrasebookState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("다운로드된 언어가 없습니다", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("언어 관리 화면에서 언어를 다운로드하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
