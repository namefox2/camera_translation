package com.example.artranslator.feature.phrasebook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Travel phrasebook screen.
 * Shows tabs for each downloaded language; within each language, phrases are grouped by category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhrasebookScreen(
    contextTabLabel: String = "회화집",
    viewModel: PhrasebookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.downloadedLanguages.isEmpty()) {
        EmptyPhrasebookState()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.phrases) { phrase ->
                PhraseCard(
                    phrase = phrase,
                    onSpeak = { viewModel.speakPhrase(phrase) }
                )
            }
        }
    }
}

@Composable
private fun PhraseCard(
    phrase: PhraseItem,
    onSpeak: () -> Unit
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
