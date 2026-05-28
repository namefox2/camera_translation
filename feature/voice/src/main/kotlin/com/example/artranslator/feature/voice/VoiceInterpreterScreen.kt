package com.example.artranslator.feature.voice

import android.Manifest
import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artranslator.core.translation.TranslationQuotaManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceInterpreterScreen(
    viewModel: VoiceInterpreterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val context = LocalContext.current

    // Collect one-shot effects (e.g. show rewarded ad)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is VoiceEffect.ShowRewardedAd -> {
                    val activity = context as? Activity
                    loadAndShowRewardedAd(activity) { viewModel.onAdRewarded() }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        Spacer(Modifier.height(16.dp))

        // Speaking text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("인식된 텍스트", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.recognizedText.ifEmpty { "마이크 버튼을 누르고 말하세요" },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Arrow down
        Icon(Icons.Default.ArrowDownward, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(8.dp))

        // Translated text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("번역 결과", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        if (uiState.translatedText.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (uiState.isOfflineTranslation)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (uiState.isOfflineTranslation) "📱 온디바이스" else "☁️ DeepL",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (uiState.isOfflineTranslation)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.translatedText.ifEmpty { "번역이 여기에 표시됩니다" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (uiState.isOfflineTranslation && uiState.translatedText.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "온디바이스 번역은 단어 위주 — 문장은 Cloud 연결 시 정확도 향상",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                if (uiState.translatedText.isNotEmpty()) {
                    IconButton(onClick = viewModel::speakTranslation) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "발음 듣기")
                    }
                }
            }
        }

        if (uiState.remainingToday <= 20) {
            Spacer(Modifier.height(6.dp))
            Text(
                "오늘 번역 가능 횟수: ${uiState.remainingToday}회",
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.remainingToday == 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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
            Spacer(Modifier.height(6.dp))
            Text(error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))
    }

    // Quota exhausted dialog
    if (uiState.showQuotaExhausted) {
        AlertDialog(
            onDismissRequest = viewModel::dismissQuotaDialog,
            title = { Text("오늘 번역 횟수를 모두 사용했습니다") },
            text = {
                Column {
                    Text("하루 ${TranslationQuotaManager.DAILY_FREE}회 무료 번역을 모두 사용했어요.")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "광고를 시청하면 ${TranslationQuotaManager.AD_GRANT}회를 추가로 사용할 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val activity = context as? Activity
                    loadAndShowRewardedAd(activity) { viewModel.onAdRewarded() }
                }) { Text("광고 보고 ${TranslationQuotaManager.AD_GRANT}회 추가") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissQuotaDialog) { Text("닫기") }
            }
        )
    }
}

private fun loadAndShowRewardedAd(activity: Activity?, onRewarded: () -> Unit) {
    activity ?: return
    RewardedAd.load(
        activity,
        "ca-app-pub-3940256099942544/5224354917", // 테스트 ID — 출시 전 실제 ID로 교체
        AdRequest.Builder().build(),
        object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                ad.show(activity) { _ -> onRewarded() }
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                // 광고 로드 실패 시에도 보상 지급 (개발 중)
                onRewarded()
            }
        }
    )
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
            Canvas(modifier = Modifier.size(96.dp)) {
                drawCircle(
                    color = buttonColor.copy(alpha = 0.3f),
                    radius = size.minDimension / 2 * scale
                )
            }
        }

        Button(
            onClick = onClick,
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
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
    val languages = remember {
        listOf(
            "ko" to "한국어", "en" to "영어", "ja" to "일본어",
            "zh" to "중국어", "fr" to "프랑스어", "de" to "독일어",
            "es" to "스페인어", "th" to "태국어"
        )
    }

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
