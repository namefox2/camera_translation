package com.example.artranslator.feature.ar

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.example.artranslator.feature.ar.TextAnalyzer.Companion.displayName
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
                    onScriptDetected = { script ->
                        // AUTO 모드에서 감지된 스크립트 → ViewModel 업데이트 → key() 재생성
                        viewModel.setSourceScript(script)
                    }
                )
                ArControlsOverlay(
                    uiState = uiState,
                    onTargetLanguageChange = viewModel::setTargetLanguage,
                    onSourceScriptChange = viewModel::setSourceScript,
                    onSwapLanguages = viewModel::swapLanguages,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                NetworkBadge(
                    isOnline = uiState.isOnline,
                    errorMessage = uiState.errorMessage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 16.dp)
                )
                // 화면 고정 / 해제 버튼
                FreezeButton(
                    isFrozen = uiState.isFrozen,
                    onClick = viewModel::toggleFreeze,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 24.dp, end = 16.dp)
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
    onScriptDetected: (TextAnalyzer.Script) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var overlayViewRef: OverlayView? by remember { mutableStateOf(null) }
    var analyzerRef: TextAnalyzer? by remember { mutableStateOf(null) }
    val analyzerExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Preview use case + PreviewView refs for clearSurfaceProvider freeze
    val previewRef = remember { arrayOfNulls<Preview>(1) }
    val previewViewRef = remember { arrayOfNulls<PreviewView>(1) }

    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    LaunchedEffect(uiState.translatedBlocks, uiState.frameWidth, uiState.frameHeight, overlayColor) {
        overlayViewRef?.setOverlayColor(overlayColor)
        overlayViewRef?.updateBlocks(uiState.translatedBlocks, uiState.frameWidth, uiState.frameHeight)
    }

    LaunchedEffect(uiState.isFrozen) {
        if (uiState.isFrozen) {
            // 프리뷰 중단: setSurfaceProvider(null)로 새 프레임 전송 중지
            // TextureView(COMPATIBLE 모드)는 GPU 텍스처를 유지해 마지막 프레임이 그대로 보임
            previewRef[0]?.setSurfaceProvider(ContextCompat.getMainExecutor(context), null)
            // 잠금 후 2.5초간 ImageAnalysis는 계속 실행 → 화면의 모든 텍스트 번역
            delay(2500)
            analyzerRef?.pause()
        } else {
            analyzerRef?.resume()
            // 프리뷰 재개
            previewViewRef[0]?.let { pv ->
                previewRef[0]?.setSurfaceProvider(pv.surfaceProvider)
            }
        }
    }

    key(uiState.sourceScript) {
        val currentScript = uiState.sourceScript

        val analyzer = remember {
            TextAnalyzer(
                script = currentScript,
                onTextDetected = { blocks, w, h -> onTextBlocksDetected(blocks, w, h) },
                onScriptDetected = { detected -> onScriptDetected(detected) }
            )
        }

        androidx.compose.runtime.SideEffect { analyzerRef = analyzer }

        DisposableEffect(Unit) {
            onDispose { analyzerRef = null; analyzer.shutdown() }
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
                    // clearSurfaceProvider 동작에 TextureView 모드 필요
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val overlayView = OverlayView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                overlayViewRef = overlayView
                previewViewRef[0] = previewView
                container.addView(previewView)
                container.addView(overlayView)
                previewRef[0] = bindCamera(ctx, lifecycleOwner, previewView, analyzerExecutor, analyzer)
                container
            },
            update = { _ -> /* 오버레이는 LaunchedEffect로 처리 */ },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    executor: ExecutorService,
    textAnalyzer: TextAnalyzer
): Preview {
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
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
    return preview
}

// ─── 화면 고정 버튼 ──────────────────────────────────────────────────────────

@Composable
private fun FreezeButton(
    isFrozen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isFrozen)
            MaterialTheme.colorScheme.primary
        else
            Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White
    ) {
        Icon(
            imageVector = if (isFrozen) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = if (isFrozen) "번역 재개" else "화면 고정"
        )
    }
}

// ─── 상단 언어 선택 컨트롤 ────────────────────────────────────────────────────

@Composable
private fun ArControlsOverlay(
    uiState: ArUiState,
    onTargetLanguageChange: (String) -> Unit,
    onSourceScriptChange: (TextAnalyzer.Script) -> Unit,
    onSwapLanguages: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val targetLanguages = listOf(
        "ko" to "한국어",
        "en" to "영어",
        "ja" to "일본어",
        "zh" to "중국어",
        "fr" to "프랑스어",
        "de" to "독일어",
        "es" to "스페인어"
    )

    val sourceOptions = listOf(
        TextAnalyzer.Script.AUTO     to "🔍 자동 감지",
        TextAnalyzer.Script.JAPANESE to "일본어",
        TextAnalyzer.Script.CHINESE  to "중국어",
        TextAnalyzer.Script.KOREAN   to "한국어",
        TextAnalyzer.Script.LATIN    to "영어 계열 (영·불·독·스)"
    )

    val sourceLabel = uiState.sourceScript.displayName()
    val targetLabel = targetLanguages.find { it.first == uiState.targetLanguage }?.second ?: "한국어"

    // ── "일본어 → 한국어" 통합 언어 선택 바 ──────────────────────────────────
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
            tonalElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // 원문 언어 (왼쪽)
                TextButton(
                    onClick = { showSourcePicker = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        sourceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 언어 교환 버튼 (AUTO 모드일 때 비활성)
                val canSwap = uiState.sourceScript != TextAnalyzer.Script.AUTO
                IconButton(
                    onClick = onSwapLanguages,
                    enabled = canSwap,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "언어 교환",
                        modifier = Modifier.size(16.dp),
                        tint = if (canSwap)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }

                // 번역 대상 언어 (오른쪽)
                TextButton(
                    onClick = { showTargetPicker = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        targetLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // ── 원문 언어 선택 다이얼로그 ─────────────────────────────────────────────
    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("원문 언어") },
            text = {
                Column {
                    Text(
                        "카메라로 읽을 텍스트의 언어를 선택하세요.\n'자동 감지'는 일본어·중국어·영어 계열을 자동으로 인식합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    sourceOptions.forEach { (script, label) ->
                        TextButton(
                            onClick = { onSourceScriptChange(script); showSourcePicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                label,
                                color = if (script == uiState.sourceScript)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // ── 번역 대상 언어 선택 다이얼로그 ───────────────────────────────────────
    if (showTargetPicker) {
        AlertDialog(
            onDismissRequest = { showTargetPicker = false },
            title = { Text("번역 대상 언어") },
            text = {
                Column {
                    targetLanguages.forEach { (code, name) ->
                        TextButton(
                            onClick = { onTargetLanguageChange(code); showTargetPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                name,
                                color = if (code == uiState.targetLanguage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// ─── 기타 컴포넌트 ────────────────────────────────────────────────────────────

@Composable
private fun NetworkBadge(
    isOnline: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Surface(
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
        if (!isOnline && errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight
                )
            }
        }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
