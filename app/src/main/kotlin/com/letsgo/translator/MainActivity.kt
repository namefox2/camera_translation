package com.letsgo.translator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.letsgo.translator.core.ui.theme.ARTranslatorTheme
import com.letsgo.translator.legal.FirstLaunchViewModel
import com.letsgo.translator.legal.PermissionNoticeDialog
import com.letsgo.translator.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val firstLaunchViewModel: FirstLaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
