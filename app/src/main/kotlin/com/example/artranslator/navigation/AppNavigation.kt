package com.example.artranslator.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.artranslator.core.ui.theme.*
import com.example.artranslator.feature.ar.ArTranslationScreen
import com.example.artranslator.feature.language.LanguageManagerScreen
import com.example.artranslator.feature.phrasebook.PhrasebookScreen
import com.example.artranslator.feature.text.TextTranslationScreen
import com.example.artranslator.feature.voice.VoiceInterpreterScreen

// ─── Navigation routes ────────────────────────────────────────────────────────

object Routes {
    const val AR = "ar"
    const val TEXT = "text"
    const val VOICE = "voice"
    const val PHRASEBOOK = "phrasebook"
    const val LANGUAGE = "language"
    const val SETTINGS = "settings"
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    themeType: ThemeType,
    onThemeChange: (ThemeType) -> Unit
) {
    val navController = rememberNavController()

    val navItems = listOf(
        NavItem(Routes.AR, "AR 번역", Icons.Default.CameraAlt),
        NavItem(Routes.TEXT, "텍스트", Icons.Default.TextFields),
        NavItem(Routes.VOICE, "음성", Icons.Default.Mic),
        NavItem(Routes.PHRASEBOOK, themeType.contextTabLabel(), Icons.Default.MenuBook),
        NavItem(Routes.LANGUAGE, "언어", Icons.Default.Download),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AR Translator",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.AR,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.AR) {
                val bubbleColor = themeType.overlayBubbleColor()
                ArTranslationScreen(
                    overlayColor = android.graphics.Color.argb(
                        (bubbleColor.alpha * 255).toInt(),
                        (bubbleColor.red * 255).toInt(),
                        (bubbleColor.green * 255).toInt(),
                        (bubbleColor.blue * 255).toInt()
                    )
                )
            }
            composable(Routes.TEXT) {
                TextTranslationScreen()
            }
            composable(Routes.VOICE) {
                VoiceInterpreterScreen()
            }
            composable(Routes.PHRASEBOOK) {
                PhrasebookScreen(contextTabLabel = themeType.contextTabLabel())
            }
            composable(Routes.LANGUAGE) {
                LanguageManagerScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    currentTheme = themeType,
                    onThemeChange = onThemeChange,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ─── Settings Screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    currentTheme: ThemeType,
    onThemeChange: (ThemeType) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("테마 선택", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            ThemeType.entries.forEach { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = theme == currentTheme,
                    onClick = { onThemeChange(theme) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: ThemeType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, accentColor, description) = when (theme) {
        ThemeType.DEFAULT -> Triple(
            Color(0xFF0D0D0D), Color(0xFF534AB7),
            "퍼플 다크 · 야간 사용 최적화"
        )
        ThemeType.BUSINESS -> Triple(
            Color(0xFF0D1520), Color(0xFF4A7FCC),
            "딥 네이비 · 전문적인 비즈니스 환경"
        )
        ThemeType.TRAVEL -> Triple(
            Color(0xFF0D0D0D), Color(0xFFD4A847),
            "앰버 골드 · 빈티지 여행 감성"
        )
        ThemeType.STUDY_ABROAD -> Triple(
            Color(0xFFF0FAF5), Color(0xFF1D9E75),
            "민트 그린 · 캠퍼스 학습 환경"
        )
    }

    val borderColor = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(bgColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color preview dot
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(accentColor, RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(theme.displayName(), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }

        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = "선택됨",
                tint = accentColor)
        }
    }
}

private fun ThemeType.displayName() = when (this) {
    ThemeType.DEFAULT -> "기본 테마"
    ThemeType.BUSINESS -> "비즈니스 테마"
    ThemeType.TRAVEL -> "여행 테마"
    ThemeType.STUDY_ABROAD -> "유학 테마"
}
