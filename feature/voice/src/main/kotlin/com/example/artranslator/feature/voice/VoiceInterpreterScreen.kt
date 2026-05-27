package com.example.artranslator.feature.voice

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceInterpreterScreen(
    viewModel: VoiceInterpreterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("실시간 음성 통역", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        // ─── 통신비밀보호법 고지 (법적 의무) ───────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "본인 발화 입력 전용 · 타인 동의 없는 녹음은 통신비밀보호법 위반",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Language swap row
        LanguagePairRow(
            sourceLanguage = uiState.sourceLanguage,
            targetLanguage = uiState.targetLanguage,
            onSwap = viewModel::swapLanguages,
            onSourceChanged = viewModel::setSourceLanguage,
            onTargetChanged = viewModel::setTargetLanguage
        )

        Spacer(Modifier.height(32.dp))

        // Speaking text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("인식된 텍스트", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.recognizedText.ifEmpty { "마이크 버튼을 누르고 말하세요" },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Arrow down
        Icon(Icons.Default.ArrowDownward, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(16.dp))

        // Translated text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("번역 결과", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.translatedText.ifEmpty { "번역이 여기에 표시됩니다" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (uiState.translatedText.isNotEmpty()) {
                    IconButton(onClick = viewModel::speakTranslation) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "발음 듣기")
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Mic button
        if (micPermission.status.isGranted) {
            MicButton(
                isListening = uiState.isListening,
                isLoading = uiState.isLoading,
                onClick = {
                    if (uiState.isListening) viewModel.stopListening()
                    else viewModel.startListening()
                }
            )
        } else {
            Button(onClick = { micPermission.launchPermissionRequest() }) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("마이크 권한 허용")
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MicButton(
    isListening: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val buttonColor = if (isListening)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        if (isListening) {
            // Pulse ring
            Canvas(modifier = Modifier.size(100.dp)) {
                drawCircle(
                    color = buttonColor.copy(alpha = 0.3f),
                    radius = size.minDimension / 2 * scale
                )
            }
        }

        Button(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "중지" else "녹음 시작",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguagePairRow(
    sourceLanguage: String,
    targetLanguage: String,
    onSwap: () -> Unit,
    onSourceChanged: (String) -> Unit,
    onTargetChanged: (String) -> Unit
) {
    val languages = listOf(
        "ko" to "한국어", "en" to "영어", "ja" to "일본어",
        "zh" to "중국어", "fr" to "프랑스어", "de" to "독일어",
        "es" to "스페인어", "th" to "태국어"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var srcExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { srcExpanded = true }) {
                Text(languages.find { it.first == sourceLanguage }?.second ?: sourceLanguage)
            }
            DropdownMenu(expanded = srcExpanded, onDismissRequest = { srcExpanded = false }) {
                languages.forEach { (code, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = {
                        onSourceChanged(code); srcExpanded = false
                    })
                }
            }
        }

        IconButton(onClick = onSwap) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "언어 교환")
        }

        var tgtExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { tgtExpanded = true }) {
                Text(languages.find { it.first == targetLanguage }?.second ?: targetLanguage)
            }
            DropdownMenu(expanded = tgtExpanded, onDismissRequest = { tgtExpanded = false }) {
                languages.forEach { (code, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = {
                        onTargetChanged(code); tgtExpanded = false
                    })
                }
            }
        }
    }
}
