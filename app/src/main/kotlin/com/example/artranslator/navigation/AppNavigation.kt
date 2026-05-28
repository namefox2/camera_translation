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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.artranslator.DonationViewModel
import com.example.artranslator.core.ui.theme.*
import com.example.artranslator.feature.ar.ArTranslationScreen
import com.example.artranslator.feature.ar.CameraTranslateScreen
import com.example.artranslator.feature.language.LanguageManagerScreen
import com.example.artranslator.feature.phrasebook.PhrasebookScreen
import com.example.artranslator.feature.text.TextTranslationScreen
import com.example.artranslator.feature.voice.VoiceInterpreterScreen
import com.example.artranslator.legal.OpenSourceLicensesScreen
import com.example.artranslator.legal.PrivacyPolicyScreen
import com.example.artranslator.legal.TermsOfServiceScreen

// ─── 라우트 상수 ──────────────────────────────────────────────────────────────

object Routes {
    const val AR               = "ar"
    const val CAMERA_TRANSLATE = "camera_translate"
    const val TEXT             = "text"
    const val VOICE            = "voice"
    const val PHRASEBOOK       = "phrasebook"
    const val LANGUAGE         = "language"
    const val SETTINGS         = "settings"
    const val PRIVACY_POLICY   = "privacy_policy"
    const val TERMS_OF_SERVICE = "terms_of_service"
    const val OSS_LICENSES     = "oss_licenses"
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

// ─── 루트 화면 제목 매핑 ──────────────────────────────────────────────────────

private fun routeTitle(route: String?, themeType: ThemeType): String = when (route) {
    Routes.AR               -> "AR 번역"
    Routes.CAMERA_TRANSLATE -> "카메라 번역"
    Routes.TEXT             -> "텍스트 번역"
    Routes.VOICE            -> "음성 번역"
    Routes.PHRASEBOOK       -> themeType.contextTabLabel()
    Routes.LANGUAGE         -> "언어 관리"
    Routes.SETTINGS         -> "설정"
    Routes.PRIVACY_POLICY   -> "개인정보처리방침"
    Routes.TERMS_OF_SERVICE -> "서비스 이용약관"
    Routes.OSS_LICENSES     -> "오픈소스 라이선스"
    else                    -> "AR Translator"
}

// ─── 메인 네비게이션 ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    themeType: ThemeType,
    onThemeChange: (ThemeType) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        NavItem(Routes.AR,               "AR 번역",    Icons.Default.CameraAlt),
        NavItem(Routes.CAMERA_TRANSLATE, "카메라 번역", Icons.Default.PhotoCamera),
        NavItem(Routes.VOICE,            "음성 번역",   Icons.Default.Mic),
        NavItem(Routes.PHRASEBOOK,       themeType.contextTabLabel(), Icons.Default.MenuBook),
        NavItem(Routes.LANGUAGE,         "언어",        Icons.Default.Download),
    )

    // 하단 탭이 있는 최상위 라우트
    val topLevelRoutes = navItems.map { it.route }.toSet()
    val isTopLevel = currentRoute in topLevelRoutes

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        routeTitle(currentRoute, themeType),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                        }
                    }
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
            if (isTopLevel) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, maxLines = 1,
                                style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute == item.route,
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
        }
    ) { innerPadding ->
        val bubbleColor = themeType.overlayBubbleColor()
        val overlayColorInt = android.graphics.Color.argb(
            (bubbleColor.alpha * 255).toInt(),
            (bubbleColor.red   * 255).toInt(),
            (bubbleColor.green * 255).toInt(),
            (bubbleColor.blue  * 255).toInt()
        )

        NavHost(
            navController = navController,
            startDestination = Routes.AR,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.AR) {
                ArTranslationScreen(overlayColor = overlayColorInt)
            }
            composable(Routes.CAMERA_TRANSLATE) {
                CameraTranslateScreen(overlayColor = overlayColorInt)
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
                    onPrivacyPolicy   = { navController.navigate(Routes.PRIVACY_POLICY) },
                    onTermsOfService  = { navController.navigate(Routes.TERMS_OF_SERVICE) },
                    onOssLicenses     = { navController.navigate(Routes.OSS_LICENSES) }
                )
            }
            composable(Routes.PRIVACY_POLICY) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TERMS_OF_SERVICE) {
                TermsOfServiceScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.OSS_LICENSES) {
                OpenSourceLicensesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// ─── 설정 화면 ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    currentTheme: ThemeType,
    onThemeChange: (ThemeType) -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsOfService: () -> Unit,
    onOssLicenses: () -> Unit
) {
    val donationViewModel: DonationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val products by donationViewModel.products.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        donationViewModel.toast.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // ─ 후원 섹션 ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Text("개발자 후원하기", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "번역 어플을 이용해 주셔서 감사합니다.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "후원금은 개발자에게 큰 힘이 됩니다 🙏",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            products.forEach { product ->
                                FilledTonalButton(
                                    onClick = {
                                        donationViewModel.donate(
                                            context as android.app.Activity, product
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        product.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─ 테마 섹션 ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
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

            // ─ 법적 고지 섹션 ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text("법적 고지", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                LegalMenuItem(Icons.Default.PrivacyTip, "개인정보처리방침", onPrivacyPolicy)
                LegalMenuItem(Icons.Default.Description, "서비스 이용약관",  onTermsOfService)
                LegalMenuItem(Icons.Default.Code,        "오픈소스 라이선스", onOssLicenses)

                Spacer(Modifier.height(8.dp))
                Text("앱 버전 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 8.dp))
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── 공통 UI 컴포넌트 ─────────────────────────────────────────────────────────

@Composable
private fun LegalMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
private fun ThemeCard(theme: ThemeType, isSelected: Boolean, onClick: () -> Unit) {
    val (bgColor, accentColor, description) = when (theme) {
        ThemeType.DEFAULT      -> Triple(Color(0xFF0D0D0D), Color(0xFF534AB7), "퍼플 다크 · 야간 사용 최적화")
        ThemeType.BUSINESS     -> Triple(Color(0xFF0D1520), Color(0xFF4A7FCC), "딥 네이비 · 전문적인 비즈니스")
        ThemeType.TRAVEL       -> Triple(Color(0xFF0D0D0D), Color(0xFFD4A847), "앰버 골드 · 빈티지 여행 감성")
        ThemeType.STUDY_ABROAD -> Triple(Color(0xFFF0FAF5), Color(0xFF1D9E75), "민트 그린 · 캠퍼스 학습 환경")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(bgColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            Icon(Icons.Default.CheckCircle, contentDescription = "선택됨", tint = accentColor)
        }
    }
}

private fun ThemeType.displayName() = when (this) {
    ThemeType.DEFAULT      -> "기본 테마"
    ThemeType.BUSINESS     -> "비즈니스 테마"
    ThemeType.TRAVEL       -> "여행 테마"
    ThemeType.STUDY_ABROAD -> "유학 테마"
}
