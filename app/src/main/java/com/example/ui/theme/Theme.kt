package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.models.AppThemeMode

private val WhatsAppDarkColorScheme = darkColorScheme(
    primary = WhatsAppGreen,
    onPrimary = Color(0xFF0B141A),
    primaryContainer = WhatsAppBubbleSentDark,
    onPrimaryContainer = WhatsAppTextPrimaryDark,
    secondary = WhatsAppLightGreen,
    onSecondary = Color(0xFF0B141A),
    secondaryContainer = WhatsAppDarkSurfaceVariant,
    onSecondaryContainer = WhatsAppTextPrimaryDark,
    tertiary = WhatsAppCheckmarkBlue,
    background = WhatsAppDarkBg,
    onBackground = WhatsAppTextPrimaryDark,
    surface = WhatsAppDarkSurface,
    onSurface = WhatsAppTextPrimaryDark,
    surfaceVariant = WhatsAppDarkSurfaceVariant,
    onSurfaceVariant = WhatsAppTextSecondaryDark,
    outline = WhatsAppDividerDark
)

private val WhatsAppLightColorScheme = lightColorScheme(
    primary = WhatsAppLightSurface,
    onPrimary = Color.White,
    primaryContainer = WhatsAppBubbleSentLight,
    onPrimaryContainer = WhatsAppTextPrimaryLight,
    secondary = WhatsAppGreen,
    onSecondary = Color.White,
    secondaryContainer = WhatsAppLightSurfaceVariant,
    onSecondaryContainer = WhatsAppTextPrimaryLight,
    tertiary = WhatsAppCheckmarkBlue,
    background = WhatsAppLightBg,
    onBackground = WhatsAppTextPrimaryLight,
    surface = WhatsAppLightBg,
    onSurface = WhatsAppTextPrimaryLight,
    surfaceVariant = WhatsAppLightSurfaceVariant,
    onSurfaceVariant = WhatsAppTextSecondaryLight,
    outline = WhatsAppDividerLight
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK_NEON,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK_NEON -> true
        AppThemeMode.CLEAN_WHITE -> false
        AppThemeMode.SYSTEM_AUTO -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) WhatsAppDarkColorScheme else WhatsAppLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
