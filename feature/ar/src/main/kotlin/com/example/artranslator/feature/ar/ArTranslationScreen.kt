package com.example.artranslator.feature.ar

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ArTranslationScreen(
    overlayColor: Int,
    viewModel: ArTranslationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.setCameraPermissionGranted(cameraPermissionState.status.isGranted)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            cameraPermissionState.status.isGranted -> {
                CameraPreviewWithOverlay(
                    uiState = uiState,
                    overlayColor = overlayColor,
                    onTextBlocksDetected = { blocks, w, h ->
                        viewModel.onTextBlocksDetected(blocks, w, h)
                    },
                    onScriptChange = { script ->
                        viewModel.setSourceScript(script)
                    }
                )
                ArControlsOverlay(
                    uiState = uiState,
                    onTargetLanguageChange = viewModel::setTargetLanguage,
                    onSourceScriptChange = viewModel::setSourceScript,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                NetworkBadge(
                    isOnline = uiState.isOnline,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 16.dp)
                )
            }
            cameraPermissionState.status.shouldShowRationale ->
                CameraPermissionRationale(onRequest = { cameraPermissionState.launchPermissionRequest() })
            else ->
                CameraPermissionRequest(onRequest = { cameraPermissionState.launchPermissionRequest() })
        }
    }
}

// ─── Camera + Overlay ─────────────────────────────────────────────────────────

@Composable
private fun CameraPreviewWithOverlay(
    uiState: ArUiState,
    overlayColor: Int,
    onTextBlocksDetected: (List<TextAnalyzer.TextBlock>, Int, Int) -> Unit,
    onScriptChange: (TextAnalyzer.Script) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var overlayViewRef: OverlayView? by remember { mutableStateOf(null) }
    val textAnalyzerRef = remember { mutableStateOf<TextAnalyzer?>(null) }
    val analyzerExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analyzerExecutor.shutdown()
            textAnalyzerRef.value?.shutdown()
        }
    }

    // 스크립트 변경 시 인식기 전환
    LaunchedEffect(uiState.sourceScript) {
        textAnalyzerRef.value?.setScript(uiState.sourceScript)
    }

    // 번역 결과 반영
    LaunchedEffect(uiState.translatedBlocks, uiState.frameWidth, uiState.frameHeight, overlayColor) {
        overlayViewRef?.setOverlayColor(overlayColor)
        overlayViewRef?.updateBlocks(uiState.translatedBlocks, uiState.frameWidth, uiState.frameHeight)
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            val container = android.widget.FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val overlayView = OverlayView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            overlayViewRef = overlayView
            container.addView(previewView)
            container.addView(overlayView)

            val analyzer = TextAnalyzer { blocks, w, h ->
                onTextBlocksDetected(blocks, w, h)
            }
            textAnalyzerRef.value = analyzer

            bindCamera(ctx, lifecycleOwner, previewView, analyzerExecutor, analyzer)
            container
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    executor: ExecutorService,
    textAnalyzer: TextAnalyzer
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, textAnalyzer) }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

// ─── 상단 컨트롤 ──────────────────────────────────────────────────────────────

@Composable
private fun ArControlsOverlay(
    uiState: ArUiState,
    onTargetLanguageChange: (String) -> Unit,
    onSourceScriptChange: (TextAnalyzer.Script) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTargetPicker by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }

    val targetLanguages = listOf(
        "ko" to "→ 한국어",
        "en" to "→ 영어",
        "ja" to "→ 일본어",
        "zh" to "→ 중국어",
        "fr" to "→ 프랑스어",
        "de" to "→ 독일어",
        "es" to "→ 스페인어"
    )

    val sourceScripts = listOf(
        TextAnalyzer.Script.LATIN    to "영·불·독·스",
        TextAnalyzer.Script.JAPANESE to "일본어",
        TextAnalyzer.Script.CHINESE  to "중국어",
        TextAnalyzer.Script.KOREAN   to "한국어"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 원문 스크립트 (인식기 선택)
        FilledTonalButton(onClick = { showSourcePicker = true }) {
            Icon(Icons.Default.TextFields, contentDescription = null,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(sourceScripts.find { it.first == uiState.sourceScript }?.second ?: "영·불·독·스",
                style = MaterialTheme.typography.labelMedium)
        }

        // 번역 목표 언어
        FilledTonalButton(onClick = { showTargetPicker = true }) {
            Icon(Icons.Default.Language, contentDescription = null,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(targetLanguages.find { it.first == uiState.targetLanguage }?.second ?: "→ 한국어",
                style = MaterialTheme.typography.labelMedium)
        }
    }

    // 원문 스크립트 선택 다이얼로그
    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("원문 언어 선택") },
            text = {
                Column {
                    Text("인식할 원문 언어를 선택하면 더 정확하게 인식됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    sourceScripts.forEach { (script, label) ->
                        TextButton(
                            onClick = { onSourceScriptChange(script); showSourcePicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 번역 목표 언어 선택 다이얼로그
    if (showTargetPicker) {
        AlertDialog(
            onDismissRequest = { showTargetPicker = false },
            title = { Text("번역할 언어 선택") },
            text = {
                Column {
                    targetLanguages.forEach { (code, name) ->
                        TextButton(
                            onClick = { onTargetLanguageChange(code); showTargetPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(name) }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// ─── 기타 컴포넌트 ────────────────────────────────────────────────────────────

@Composable
private fun NetworkBadge(isOnline: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (isOnline) MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                else          MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = if (isOnline) "온라인" else "오프라인",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CameraPermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("카메라 권한이 필요합니다", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("권한 허용") }
    }
}

@Composable
private fun CameraPermissionRationale(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "AR 번역 기능을 사용하려면 카메라 접근 권한이 필요합니다.\n카메라로 텍스트를 실시간으로 인식하여 번역합니다.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("권한 허용") }
    }
}
