package com.example.artranslator.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Supported application themes.
 * Each maps to a distinct color palette, typography and AR overlay color.
 */
enum class ThemeType {
    DEFAULT,      // Purple dark – app default
    BUSINESS,     // Deep navy / cobalt – professional use
    TRAVEL,       // Amber gold / parchment – traveller feel
    STUDY_ABROAD  // Mint green / teal – campus / education
}

// ─── Color schemes ────────────────────────────────────────────────────────────

private val DefaultColorScheme: ColorScheme = darkColorScheme(
    primary = DefaultPrimary,
    onPrimary = DefaultOnPrimary,
    secondary = DefaultSecondary,
    onSecondary = DefaultOnPrimary,
    background = DefaultBackground,
    onBackground = DefaultOnBackground,
    surface = DefaultSurface,
    onSurface = DefaultOnBackground,
)

private val BusinessColorScheme: ColorScheme = darkColorScheme(
    primary = BusinessPrimary,
    onPrimary = BusinessOnPrimary,
    secondary = BusinessSecondary,
    onSecondary = BusinessOnPrimary,
    background = BusinessBackground,
    onBackground = BusinessOnBackground,
    surface = BusinessSurface,
    onSurface = BusinessOnBackground,
)

private val TravelColorScheme: ColorScheme = darkColorScheme(
    primary = TravelPrimary,
    onPrimary = TravelOnPrimary,
    secondary = TravelSecondary,
    onSecondary = Color(0xFF0D0D0D),
    background = TravelBackground,
    onBackground = TravelOnBackground,
    surface = TravelSurface,
    onSurface = TravelOnBackground,
)

private val StudyColorScheme: ColorScheme = lightColorScheme(
    primary = StudyPrimary,
    onPrimary = StudyOnPrimary,
    secondary = StudySecondary,
    onSecondary = StudyOnBackground,
    background = StudyBackground,
    onBackground = StudyOnBackground,
    surface = StudySurface,
    onSurface = StudyOnBackground,
)

/**
 * Returns the AR overlay bubble color for the active theme.
 */
fun ThemeType.overlayBubbleColor(): Color = when (this) {
    ThemeType.DEFAULT -> DefaultOverlayBubble
    ThemeType.BUSINESS -> BusinessOverlayBubble
    ThemeType.TRAVEL -> TravelOverlayBubble
    ThemeType.STUDY_ABROAD -> StudyOverlayBubble
}

/**
 * Returns the tab label for the context-specific tab (5th tab).
 * Each theme provides a different name / context.
 */
fun ThemeType.contextTabLabel(): String = "회화"

@Composable
fun ARTranslatorTheme(
    themeType: ThemeType = ThemeType.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        ThemeType.DEFAULT -> DefaultColorScheme
        ThemeType.BUSINESS -> BusinessColorScheme
        ThemeType.TRAVEL -> TravelColorScheme
        ThemeType.STUDY_ABROAD -> StudyColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DefaultTypography,
        content = content
    )
}
