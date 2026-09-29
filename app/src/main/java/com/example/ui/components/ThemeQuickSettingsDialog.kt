package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.AppThemeMode
import com.example.data.models.BackgroundPreset
import com.example.data.models.ThemeState

@Composable
fun ThemeQuickSettingsDialog(
    themeState: ThemeState,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetBackgroundPreset: (BackgroundPreset) -> Unit,
    onSetGalleryWallpaper: (String) -> Unit,
    onGenerateAiTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var aiPromptInput by remember { mutableStateOf("") }
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSetGalleryWallpaper(uri.toString())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
            color = Color(0xFF0F172A),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tema & Wallpaper Obrolan",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Kustomisasi Tampilan Chatin",
                                fontSize = 11.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mode Tema
                Text(
                    text = "MODE WARNA TAMPILAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isDark = themeState.themeMode == AppThemeMode.DARK_NEON
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                        border = if (isDark) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSetThemeMode(AppThemeMode.DARK_NEON) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌙", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Dark Neon", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text("Aksen Cyber Blue", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }

                    val isLight = themeState.themeMode == AppThemeMode.CLEAN_WHITE
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isLight) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                        border = if (isLight) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSetThemeMode(AppThemeMode.CLEAN_WHITE) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("☀️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Tema Putih", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text("Clean Minimalis", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Wallpaper Pemandangan
                Text(
                    text = "WALLPAPER PEMANDANGAN & ALAM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val scenicPresets = listOf(
                    Triple(BackgroundPreset.SKY, "Pemandangan Langit", "☁️"),
                    Triple(BackgroundPreset.NATURE, "Pemandangan Alam", "🌿"),
                    Triple(BackgroundPreset.FOREST, "Hutan Belantara", "🌲"),
                    Triple(BackgroundPreset.OCEAN, "Lautan Biru", "🌊"),
                    Triple(BackgroundPreset.CYBER_DARK, "Gelap Cyberpunk", "⚡"),
                    Triple(BackgroundPreset.CLEAN_MINIMAL, "Putih Bersih", "❄️")
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(scenicPresets) { item ->
                        val preset = item.first
                        val name = item.second
                        val emoji = item.third
                        val isSelected = themeState.backgroundPreset == preset
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { onSetBackgroundPreset(preset) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(emoji, fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Wallpaper Galeri
                Text(
                    text = "WALLPAPER DARI GALERI PERANGKAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { galleryPicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (themeState.backgroundPreset == BackgroundPreset.CUSTOM_GALLERY) "Foto Galeri Aktif (Ganti)" else "Pilih Foto dari Galeri Ponsel",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // AI Generator
                Text(
                    text = "BUAT TEMA / WALLPAPER MENGGUNAKAN AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C4DFF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = aiPromptInput,
                    onValueChange = { aiPromptInput = it },
                    placeholder = { Text("Contoh: Kota neon cyberpunk di atas awan, nuansa ungu...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C4DFF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (aiPromptInput.isNotBlank()) {
                            onGenerateAiTheme(aiPromptInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buat Wallpaper dengan AI", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Terapkan & Selesai", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
