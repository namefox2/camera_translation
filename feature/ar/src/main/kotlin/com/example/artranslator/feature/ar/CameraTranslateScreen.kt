package com.example.artranslator.feature.ar

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.artranslator.feature.ar.TextAnalyzer.Companion.displayName
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraTranslateScreen(
    overlayColor: Int,
    viewModel: CameraTranslateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    if (!cameraPermission.status.isGranted) {
        PermissionRequestScreen { cameraPermission.launchPermissionRequest() }
        return
    }

    when (val step = uiState.step) {
        is CaptureStep.Preview -> CameraPreviewStep(
            targetLanguage = uiState.targetLanguage,
            sourceScript = uiState.sourceScript,
            overlayColor = overlayColor,
            onCaptured = { bitmap, rotation -> viewModel.onPhotoCaptured(bitmap, rotation) },
            onLanguageChange = viewModel::setTargetLanguage,
            onScriptChange = viewModel::setSourceScript
        )
        is CaptureStep.Selecting -> SelectionStep(
            bitmap = step.bitmap,
            isProcessing = uiState.isProcessing,
            error = uiState.error,
            overlayColor = overlayColor,
            onTranslate = { start, end, size ->
                viewModel.translateSelection(step.bitmap, start, end, size)
            },
            onRetake = viewModel::retake,
            onClearError = viewModel::clearError
        )
        is CaptureStep.Result -> ResultStep(
            result = step,
            overlayColor = overlayColor,
            onRetake = viewModel::retake,
            onReselect = { viewModel.reselect(step.bitmap) }
        )
    }
}

// ─── Step 1: 카메라 프리뷰 + 촬영 ─────────────────────────────────────────────

@Composable
private fun CameraPreviewStep(
    targetLanguage: String,
    sourceScript: TextAnalyzer.Script,
    overlayColor: Int,
    onCaptured: (Bitmap, Int) -> Unit,
    onLanguageChange: (String) -> Unit,
    onScriptChange: (TextAnalyzer.Script) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var showLangPicker by remember { mutableStateOf(false) }
    var showScriptPicker by remember { mutableStateOf(false) }

    val languages = listOf(
        "ko" to "한국어", "en" to "영어", "ja" to "일본어",
        "zh" to "중국어", "fr" to "프랑스어", "de" to "독일어", "es" to "스페인어"
    )

    val sourceScripts = listOf(
        TextAnalyzer.Script.AUTO     to "🔍 자동 감지",
        TextAnalyzer.Script.JAPANESE to "일본어",
        TextAnalyzer.Script.CHINESE  to "중국어",
        TextAnalyzer.Script.KOREAN   to "한국어",
        TextAnalyzer.Script.LATIN    to "영어 계열 (영·불·독·스)"
    )

    // 갤러리에서 이미지 선택
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bitmap = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) { null }
        bitmap?.let { onCaptured(it, 0) }
    }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Box(Modifier.fillMaxSize()) {
        // 카메라 프리뷰
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView ->
                    bindCaptureCamera(ctx, lifecycleOwner, previewView, executor) { capture ->
                        imageCaptureRef = capture
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 뷰파인더 가이드 (화면 가운데 흰 사각형 선)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pw = size.width * 0.75f
            val ph = size.height * 0.55f
            val left = (size.width - pw) / 2f
            val top = (size.height - ph) / 2f
            drawRect(
                color = Color.White.copy(alpha = 0.6f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(pw, ph),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 상단: "일본어 → 한국어" 통합 언어 선택 바
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                tonalElevation = 2.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    // 원문 언어 (왼쪽)
                    TextButton(
                        onClick = { showScriptPicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            sourceScript.displayName(),
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
                    // 화살표 구분자
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    // 번역 대상 언어 (오른쪽)
                    TextButton(
                        onClick = { showLangPicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            languages.find { it.first == targetLanguage }?.second ?: "한국어",
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

        // 촬영 힌트
        Text(
            "번역할 텍스트가 뷰파인더 안에 오도록 맞추고 촬영하세요",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-120).dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // 하단: 갤러리 + 셔터 버튼
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 44.dp, start = 48.dp, end = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 갤러리 버튼
            FilledTonalIconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "앨범에서 선택",
                    modifier = Modifier.size(28.dp)
                )
            }

            // 셔터 버튼 (가운데, 더 크게)
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                ) {}
                Button(
                    onClick = {
                        imageCaptureRef?.takePicture(
                            executor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bmp = image.toBitmap()
                                    val rotation = image.imageInfo.rotationDegrees
                                    image.close()
                                    onCaptured(bmp, rotation)
                                }
                                override fun onError(exception: ImageCaptureException) {}
                            }
                        )
                    },
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera, contentDescription = "촬영",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // 균형 맞추기용 빈 공간 (오른쪽)
            Spacer(Modifier.size(56.dp))
        }
    }

    // 원문 스크립트 선택 다이얼로그
    if (showScriptPicker) {
        AlertDialog(
            onDismissRequest = { showScriptPicker = false },
            title = { Text("원문 언어 선택") },
            text = {
                Column {
                    Text("찍을 텍스트의 언어를 선택하면 인식 정확도가 높아집니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    sourceScripts.forEach { (script, label) ->
                        TextButton(
                            onClick = { onScriptChange(script); showScriptPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 번역 대상 언어 선택 다이얼로그
    if (showLangPicker) {
        AlertDialog(
            onDismissRequest = { showLangPicker = false },
            title = { Text("번역 대상 언어") },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        TextButton(onClick = { onLanguageChange(code); showLangPicker = false }) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// ─── Step 2: 사진 표시 + 드래그 영역 선택 ─────────────────────────────────────

@Composable
private fun SelectionStep(
    bitmap: Bitmap,
    isProcessing: Boolean,
    error: String?,
    overlayColor: Int,
    onTranslate: (Offset, Offset, IntSize) -> Unit,
    onRetake: () -> Unit,
    onClearError: () -> Unit
) {
    var selStart by remember { mutableStateOf<Offset?>(null) }
    var selEnd by remember { mutableStateOf<Offset?>(null) }
    var viewSize by remember { mutableStateOf(IntSize(1, 1)) }
    val hasSelection = selStart != null && selEnd != null &&
        abs((selEnd?.x ?: 0f) - (selStart?.x ?: 0f)) > 10f

    val bubbleColor = Color(overlayColor).copy(alpha = 0.35f)
    val strokeColor = Color(overlayColor)

    Box(Modifier.fillMaxSize()) {
        // 촬영된 사진
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "촬영된 사진",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords -> viewSize = coords.size }
        )

        // 드래그 선택 오버레이
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isProcessing) {
                    if (isProcessing) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            selStart = offset
                            selEnd = offset
                            onClearError()
                        },
                        onDrag = { change, _ -> selEnd = change.position }
                    )
                }
        ) {
            selStart?.let { s ->
                selEnd?.let { e ->
                    val left = minOf(s.x, e.x)
                    val top = minOf(s.y, e.y)
                    val w = abs(e.x - s.x)
                    val h = abs(e.y - s.y)
                    // 반투명 채움
                    drawRect(
                        color = bubbleColor,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(w, h)
                    )
                    // 테두리
                    drawRect(
                        color = strokeColor,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // 상단 안내
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                if (hasSelection) "번역 버튼을 눌러 선택 영역을 번역하세요"
                else "번역할 텍스트 위를 손가락으로 드래그해서 선택하세요",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // 오류 메시지
        error?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // 로딩
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("텍스트 인식 중…", color = Color.White,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // 하단 버튼 영역
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onRetake,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Replay, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("다시 찍기")
            }

            Button(
                onClick = { selStart?.let { s -> selEnd?.let { e -> onTranslate(s, e, viewSize) } } },
                enabled = hasSelection && !isProcessing
            ) {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("번역하기")
            }
        }
    }
}

// ─── Step 3: 번역 결과 표시 ────────────────────────────────────────────────────

@Composable
private fun ResultStep(
    result: CaptureStep.Result,
    overlayColor: Int,
    onRetake: () -> Unit,
    onReselect: () -> Unit
) {
    val bubbleColor = Color(overlayColor).copy(alpha = 0.35f)
    val strokeColor = Color(overlayColor)

    Box(Modifier.fillMaxSize()) {
        // 사진 + 선택 영역 표시
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.Image(
                bitmap = result.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Canvas(Modifier.fillMaxSize()) {
                val s = result.selStart
                val e = result.selEnd
                val left = minOf(s.x, e.x)
                val top = minOf(s.y, e.y)
                val w = abs(e.x - s.x)
                val h = abs(e.y - s.y)
                drawRect(color = bubbleColor, topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(w, h))
                drawRect(color = strokeColor, topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    style = Stroke(width = 2.dp.toPx()))
            }
        }

        // 번역 결과 카드 (하단 슬라이드업)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 헤더
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("번역 결과", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        if (result.isOffline) {
                            AssistChip(onClick = {}, label = {
                                Text("오프라인", style = MaterialTheme.typography.labelSmall)
                            })
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 원문
                    Text("원문", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(2.dp))
                    Text(result.recognizedText, style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // 번역문
                    Text("번역", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    Spacer(Modifier.height(2.dp))
                    Text(result.translatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium)

                    Spacer(Modifier.height(20.dp))

                    // 하단 버튼
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Replay, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("다시 찍기")
                        }
                        Button(onClick = onReselect, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.SelectAll, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("다시 선택")
                        }
                    }
                }
            }
        }
    }
}

// ─── CameraX 바인딩 (ImageCapture 포함) ──────────────────────────────────────

private fun bindCaptureCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    executor: ExecutorService,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        val provider = future.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            onImageCaptureReady(imageCapture)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

// ─── 권한 요청 화면 ────────────────────────────────────────────────────────────

@Composable
private fun PermissionRequestScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("카메라 번역", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("사진을 찍고 원하는 영역만 선택해서 번역합니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("카메라 권한 허용") }
    }
}
