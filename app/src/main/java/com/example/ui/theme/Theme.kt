package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.models.AppThemeMode

private val ChatinDarkColorScheme = darkColorScheme(
    primary = WhatsAppLightGreen,
    onPrimary = Color(0xFF06231F),
    primaryContainer = WhatsAppBubbleSentDark,
    onPrimaryContainer = WhatsAppTextPrimaryDark,
    secondary = WhatsAppCheckmarkBlue,
    onSecondary = Color(0xFF071923),
    secondaryContainer = WhatsAppDarkSurfaceVariant,
    onSecondaryContainer = WhatsAppTextPrimaryDark,
    tertiary = RcsPurple,
    background = WhatsAppDarkBg,
    onBackground = WhatsAppTextPrimaryDark,
    surface = WhatsAppDarkSurface,
    onSurface = WhatsAppTextPrimaryDark,
    surfaceVariant = WhatsAppDarkSurfaceVariant,
    onSurfaceVariant = WhatsAppTextSecondaryDark,
    outline = WhatsAppDividerDark
)

private val ChatinLightColorScheme = lightColorScheme(
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
    surface = WhatsAppLightCard,
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
    MaterialTheme(
        colorScheme = if (isDark) ChatinDarkColorScheme else ChatinLightColorScheme,
        typography = Typography,
        content = content
    )
}
