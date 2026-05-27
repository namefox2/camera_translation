package com.example.artranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.artranslator.core.ui.theme.ARTranslatorTheme
import com.example.artranslator.legal.FirstLaunchViewModel
import com.example.artranslator.legal.PermissionNoticeDialog
import com.example.artranslator.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val firstLaunchViewModel: FirstLaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTheme by themeViewModel.currentTheme.collectAsState()
            val showPermissionNotice by firstLaunchViewModel.showPermissionNotice.collectAsState()

            ARTranslatorTheme(themeType = currentTheme) {
                AppNavigation(
                    themeType = currentTheme,
                    onThemeChange = themeViewModel::setTheme
                )

                // 최초 실행 시 앱 접근권한 고지 팝업 (방통위 가이드라인)
                if (showPermissionNotice) {
                    PermissionNoticeDialog(
                        onAcknowledge = firstLaunchViewModel::onPermissionNoticeAcknowledged
                    )
                }
            }
        }
    }
}
