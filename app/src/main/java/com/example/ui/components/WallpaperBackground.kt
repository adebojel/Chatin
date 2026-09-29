package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.models.AppThemeMode
import com.example.data.models.BackgroundPreset
import com.example.data.models.ThemeState
import com.example.ui.theme.BackgroundDark

@Composable
fun WallpaperBackground(
    themeState: ThemeState,
    modifier: Modifier = Modifier,
    blurRadius: Int = 0,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        val isDark = themeState.themeMode != AppThemeMode.CLEAN_WHITE
        when (themeState.backgroundPreset) {
            BackgroundPreset.CYBER_DARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF070B14),
                                    Color(0xFF0F172A),
                                    Color(0xFF151F38)
                                )
                            )
                        )
                )
            }
            BackgroundPreset.CLEAN_MINIMAL -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFF1F5F9),
                                    Color(0xFFE2E8F0)
                                )
                            )
                        )
                )
            }
            BackgroundPreset.SKY -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) listOf(Color(0xFF0C2442), Color(0xFF1B4965), Color(0xFF09172B))
                                else listOf(Color(0xFFBAE6FD), Color(0xFFE0F2FE), Color(0xFFF0F9FF))
                            )
                        )
                )
            }
            BackgroundPreset.NATURE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) listOf(Color(0xFF143628), Color(0xFF064E3B), Color(0xFF022C22))
                                else listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0), Color(0xFFECFDF5))
                            )
                        )
                )
            }
            BackgroundPreset.FOREST -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) listOf(Color(0xFF06281E), Color(0xFF064E3B), Color(0xFF0B1914))
                                else listOf(Color(0xFFC6F6D5), Color(0xFF9AE6B4), Color(0xFFEBF8FA))
                            )
                        )
                )
            }
            BackgroundPreset.OCEAN -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) listOf(Color(0xFF07213A), Color(0xFF0369A1), Color(0xFF021727))
                                else listOf(Color(0xFF7DD3FC), Color(0xFF38BDF8), Color(0xFFE0F2FE))
                            )
                        )
                )
            }
            BackgroundPreset.CUSTOM_GALLERY -> {
                if (!themeState.customGalleryUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = Uri.parse(themeState.customGalleryUri),
                        contentDescription = "Custom Gallery Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (blurRadius > 0) Modifier.blur(blurRadius.dp) else Modifier)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isDark) Color.Black.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.40f)
                        )
                )
            }
            BackgroundPreset.AI_GENERATED -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF7C4DFF).copy(alpha = 0.4f),
                                    Color(0xFF00E5FF).copy(alpha = 0.3f),
                                    if (isDark) Color(0xFF090D16) else Color(0xFFE0E7FF)
                                ),
                                radius = 1200f
                            )
                        )
                )
            }
        }
        content()
    }
}
