package com.example.artranslator.feature.screen

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTranslationScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isEnabled by remember { mutableStateOf(false) }
    var targetLanguage by remember { mutableStateOf("ko") }
    var langMenuExpanded by remember { mutableStateOf(false) }

    val languages = remember {
        listOf(
            "ko" to "한국어", "en" to "영어", "ja" to "일본어",
            "zh" to "중국어", "fr" to "프랑스어", "de" to "독일어",
            "es" to "스페인어", "th" to "태국어"
        )
    }

    // Load saved language pref
    LaunchedEffect(Unit) {
        targetLanguage = context
            .getSharedPreferences("screen_translation_prefs", Context.MODE_PRIVATE)
            .getString("target_language", "ko") ?: "ko"
    }

    // Re-check service status every time this screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        // ── 서비스 상태 카드 ──────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isEnabled)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isEnabled) "화면 번역 활성화됨" else "화면 번역 비활성화됨",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isEnabled)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isEnabled)
                            "텍스트를 길게 눌러 선택하면 번역 오버레이가 표시됩니다"
                        else
                            "아래 버튼을 눌러 접근성 서비스를 활성화해 주세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled)
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        else
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 활성화 버튼 ───────────────────────────────────────────────────────
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Accessibility, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isEnabled) "접근성 설정 열기" else "접근성 설정에서 활성화하기")
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ── 번역 대상 언어 ────────────────────────────────────────────────────
        Text(
            "번역 대상 언어",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = langMenuExpanded,
            onExpandedChange = { langMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = languages.find { it.first == targetLanguage }?.second ?: "한국어",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langMenuExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = langMenuExpanded,
                onDismissRequest = { langMenuExpanded = false }
            ) {
                languages.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            targetLanguage = code
                            langMenuExpanded = false
                            context.getSharedPreferences("screen_translation_prefs", Context.MODE_PRIVATE)
                                .edit().putString("target_language", code).apply()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ── 사용 방법 ──────────────────────────────────────────────────────────
        Text(
            "사용 방법",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        HowToStep(1, "\"접근성 설정에서 활성화하기\" 버튼을 누르세요")
        HowToStep(2, "설치된 앱 서비스 목록에서 \"AR Translator\"를 찾아 켜세요")
        HowToStep(3, "어떤 앱에서든 번역하고 싶은 텍스트를 길게 눌러 선택하세요")
        HowToStep(4, "화면 하단에 번역 결과 카드가 자동으로 나타납니다")
        HowToStep(5, "카드 제목 부분을 드래그해서 위치를 옮길 수 있고, ✕ 버튼으로 닫을 수 있습니다")
    }
}

@Composable
private fun HowToStep(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "$number",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val componentName = "${context.packageName}/.service.ScreenTranslationService"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(":").any { it.equals(componentName, ignoreCase = true) }
}
