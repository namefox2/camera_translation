package com.example.artranslator.feature.ar

import android.Manifest
import android.content.Context
import android.graphics.RectF
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.setCameraPermissionGranted(cameraPermissionState.status.isGranted)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            cameraPermissionState.status.isGranted -> {
                // Camera preview + overlay
                CameraPreviewWithOverlay(
                    uiState = uiState,
                    overlayColor = overlayColor,
                    onTextBlocksDetected = { blocks, w, h ->
                        viewModel.onTextBlocksDetected(blocks, w, h)
                    }
                )

                // Top controls
                ArControlsOverlay(
                    uiState = uiState,
                    onTargetLanguageChange = viewModel::setTargetLanguage,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Online/Offline badge
                NetworkBadge(
                    isOnline = uiState.isOnline,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
            cameraPermissionState.status.shouldShowRationale -> {
                CameraPermissionRationale(onRequest = { cameraPermissionState.launchPermissionRequest() })
            }
            else -> {
                CameraPermissionRequest(onRequest = { cameraPermissionState.launchPermissionRequest() })
            }
        }
    }
}

@Composable
private fun CameraPreviewWithOverlay(
    uiState: ArUiState,
    overlayColor: Int,
    onTextBlocksDetected: (List<TextAnalyzer.TextBlock>, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var overlayViewRef: OverlayView? by remember { mutableStateOf(null) }
    val analyzerExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    // Update overlay when translated blocks change
    LaunchedEffect(uiState.translatedBlocks, overlayColor) {
        overlayViewRef?.setOverlayColor(overlayColor)
        overlayViewRef?.updateBlocks(uiState.translatedBlocks)
    }

    AndroidView(
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

            // Bind camera
            bindCamera(ctx, lifecycleOwner, previewView, analyzerExecutor, onTextBlocksDetected)

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
    onTextBlocksDetected: (List<TextAnalyzer.TextBlock>, Int, Int) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val textAnalyzer = TextAnalyzer { blocks ->
            // Image dimensions from a standard camera are 640x480 or similar
            onTextBlocksDetected(blocks, 640, 480)
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

@Composable
private fun ArControlsOverlay(
    uiState: ArUiState,
    onTargetLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguagePicker by remember { mutableStateOf(false) }
    val languages = listOf(
        "ko" to "한국어",
        "en" to "영어",
        "ja" to "일본어",
        "zh" to "중국어",
        "fr" to "프랑스어",
        "de" to "독일어",
        "es" to "스페인어"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = { showLanguagePicker = true }
        ) {
            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(languages.find { it.first == uiState.targetLanguage }?.second ?: "한국어")
        }
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text("번역 언어 선택") },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        TextButton(
                            onClick = {
                                onTargetLanguageChange(code)
                                showLanguagePicker = false
                            }
                        ) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun NetworkBadge(isOnline: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (isOnline)
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
        else
            MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
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
            "AR 번역 기능을 사용하려면 카메라 접근 권한이 필요합니다. 카메라로 텍스트를 실시간으로 인식하여 번역합니다.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("권한 허용") }
    }
}
