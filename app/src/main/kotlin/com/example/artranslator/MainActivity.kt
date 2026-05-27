package com.example.artranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.artranslator.core.ui.theme.ARTranslatorTheme
import com.example.artranslator.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTheme by themeViewModel.currentTheme.collectAsState()

            ARTranslatorTheme(themeType = currentTheme) {
                AppNavigation(
                    themeType = currentTheme,
                    onThemeChange = themeViewModel::setTheme
                )
            }
        }
    }
}
