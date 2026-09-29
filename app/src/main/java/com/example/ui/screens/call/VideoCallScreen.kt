package com.example.ui.screens.call

import android.app.Activity
import android.app.PictureInPictureParams
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.*
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.FloatingTorchButton
import com.example.ui.viewmodels.ActiveCallState
import kotlin.math.roundToInt

@Composable
fun VideoCallScreen(
    callState: ActiveCallState,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onUpdateOptions: ((VideoCallOptions) -> VideoCallOptions) -> Unit,
    onSetGreenScreenUri: (String) -> Unit = {},
    onEndCall: () -> Unit,
    onSwitchToGsm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val options = callState.callOptions
    var isEffectsMenuOpen by remember { mutableStateOf(false) }
    var selectedEffectCategory by remember { mutableStateOf(ArEffectCategory.KEREN) }

    val greenScreenPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSetGreenScreenUri(uri.toString())
        }
    }

    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

    val durationText = remember(callState.durationSeconds) {
        val minutes = callState.durationSeconds / 60
        val seconds = callState.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    BackHandler {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activity = context as? Activity
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))
                .build()
            activity?.enterPictureInPictureMode(params)
        } else {
            onEndCall()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // Remote Video / Main Camera Stream
        if (options.isCameraOn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when (options.backgroundMode) {
                            VirtualBackgroundMode.BLUR_LIGHT -> Modifier.blur(10.dp)
                            VirtualBackgroundMode.BLUR_INTENSE -> Modifier.blur(25.dp)
                            else -> Modifier
                        }
                    )
            ) {
                CameraPreviewView(isFrontCamera = options.isFrontCamera)

                // Virtual Background & Green Screen
                when (options.backgroundMode) {
                    VirtualBackgroundMode.GREEN_SCREEN_GALLERY -> {
                        if (!options.greenScreenCustomUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = Uri.parse(options.greenScreenCustomUri),
                                contentDescription = "Green Screen Galeri",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.65f)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF00FF00).copy(alpha = 0.40f))
                            )
                        }
                    }
                    VirtualBackgroundMode.VIRTUAL_OFFICE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF1E293B).copy(alpha = 0.5f),
                                            Color(0xFF0F172A).copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                    }
                    VirtualBackgroundMode.VIRTUAL_AURORA -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                                            Color(0xFF7C4DFF).copy(alpha = 0.45f),
                                            Color(0xFF090D16).copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                    }
                    VirtualBackgroundMode.VIRTUAL_SUNSET -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFF7E5F).copy(alpha = 0.4f),
                                            Color(0xFFFEB47B).copy(alpha = 0.3f),
                                            Color(0xFF2C3E50).copy(alpha = 0.6f)
                                        )
                                    )
                                )
                        )
                    }
                    VirtualBackgroundMode.VIRTUAL_ZEN_GARDEN -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF064E3B).copy(alpha = 0.4f),
                                            Color(0xFF022C22).copy(alpha = 0.6f)
                                        )
                                    )
                                )
                        )
                    }
                    else -> {}
                }

                FilterColorShaderOverlay(options.selectedFilter)
                ArFaceEffectOverlay(options.faceEffect)
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = callState.contactName.take(1),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF090D16)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = callState.contactName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Kamera dimatikan",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Screen Sharing Active Banner
        if (options.isScreenSharing) {
            Surface(
                color = Color(0xFF8B5CF6).copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenShare,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Berbagi Layar Aktif (Presentasi HD)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Floating In-App Self PiP (Draggable)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                .padding(top = 90.dp, end = 16.dp)
                .size(width = 100.dp, height = 150.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                .background(Color(0xFF1E293B))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        pipOffsetX += dragAmount.x
                        pipOffsetY += dragAmount.y
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Anda (HD)",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .fillMaxWidth()
                        .padding(2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Top Status Header
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = callState.contactName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (callState.networkMode) {
                                        CallNetworkMode.VOIP_WIFI -> Color(0xFF10B981)
                                        CallNetworkMode.VOIP_CELLULAR -> Color(0xFF00E5FF)
                                        CallNetworkMode.GSM_OPERATOR -> Color(0xFFF59E0B)
                                    }
                                )
                        )
                        Text(
                            text = when (callState.networkMode) {
                                CallNetworkMode.VOIP_WIFI -> "Wi-Fi HD 1080p (${options.bitrateKbps} kbps)"
                                CallNetworkMode.VOIP_CELLULAR -> "5G Adaptif 720p (${options.bitrateKbps} kbps)"
                                CallNetworkMode.GSM_OPERATOR -> "GSM Seluler Fallback"
                            },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "• $durationText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FloatingTorchButton(
                        isTorchOn = isTorchOn,
                        onToggle = onToggleTorch,
                        modifier = Modifier.size(38.dp)
                    )
                    IconButton(
                        onClick = onSwitchToGsm,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFFF59E0B), CircleShape)
                            .testTag("switch_to_gsm_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Alihkan ke GSM Telepon",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val activity = context as? Activity
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(9, 16))
                                    .build()
                                activity?.enterPictureInPictureMode(params)
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Mode Picture in Picture",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Active Voice Changer Badge
        if (options.voiceChanger != VoiceChangerEffect.NORMAL) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10B981).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = options.voiceChanger.emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pengubah Suara: ${options.voiceChanger.label} (${options.voiceChanger.pitchMultiplier}x)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Floating Effect Drawer
        AnimatedVisibility(
            visible = isEffectsMenuOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Efek Kamera, Wajah & Green Screen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        IconButton(onClick = { isEffectsMenuOpen = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Efek",
                                tint = Color.White
                            )
                        }
                    }

                    // 1. Color Filters
                    Text(
                        text = "FILTER WARNA & BEAUTY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(ColorFilterType.values()) { filter ->
                            val isSelected = options.selectedFilter == filter
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    onUpdateOptions { it.copy(selectedFilter = filter) }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
                                        )
                                        .border(
                                            2.dp,
                                            if (isSelected) Color.White else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = filter.iconEmoji, fontSize = 20.sp)
                                }
                                Text(
                                    text = filter.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Virtual Backgrounds & Green Screen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LATAR BELAKANG & GREEN SCREEN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C4DFF)
                        )
                        TextButton(
                            onClick = { greenScreenPickerLauncher.launch("image/*") }
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pilih Foto Galeri", fontSize = 11.sp, color = Color(0xFF00E5FF))
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(VirtualBackgroundMode.values()) { bg ->
                            val isSelected = options.backgroundMode == bg
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF7C4DFF).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF)) else null,
                                modifier = Modifier.clickable {
                                    if (bg == VirtualBackgroundMode.GREEN_SCREEN_GALLERY && options.greenScreenCustomUri == null) {
                                        greenScreenPickerLauncher.launch("image/*")
                                    } else {
                                        onUpdateOptions { it.copy(backgroundMode = bg) }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (bg == VirtualBackgroundMode.GREEN_SCREEN_GALLERY) {
                                        Icon(
                                            imageVector = Icons.Default.Wallpaper,
                                            contentDescription = null,
                                            tint = Color(0xFF00FF00),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = bg.label,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color(0xFF7C4DFF) else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. AR Face Stickers
                    Text(
                        text = "EFEK WAJAH & AR REAL-TIME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF2A85)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(ArEffectCategory.values()) { cat ->
                            val isCatSelected = selectedEffectCategory == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCatSelected) Color(0xFFFF2A85).copy(alpha = 0.3f) else Color(0xFF1E293B),
                                border = if (isCatSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A85)) else null,
                                modifier = Modifier.clickable { selectedEffectCategory = cat }
                            ) {
                                Text(
                                    text = cat.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCatSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    val categoryEffects = remember(selectedEffectCategory) {
                        listOf(ArFaceEffect.NONE) + ArFaceEffect.values().filter { it.category == selectedEffectCategory && it != ArFaceEffect.NONE }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(categoryEffects) { face ->
                            val isSelected = options.faceEffect == face
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    onUpdateOptions { it.copy(faceEffect = face) }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFFFF2A85) else Color(0xFF1E293B)
                                        )
                                        .border(
                                            2.dp,
                                            if (isSelected) Color.White else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = face.emoji, fontSize = 20.sp)
                                }
                                Text(
                                    text = face.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color(0xFFFF2A85) else Color.White.copy(alpha = 0.7f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Voice Changer
                    Text(
                        text = "PENGUBAH SUARA REAL-TIME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(VoiceChangerEffect.values()) { voice ->
                            val isSelected = options.voiceChanger == voice
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF1E293B),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)) else null,
                                modifier = Modifier.clickable {
                                    onUpdateOptions { it.copy(voiceChanger = voice) }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = voice.emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = voice.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF10B981) else Color.White
                                        )
                                        Text(
                                            text = "${voice.pitchMultiplier}x pitch",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls Bar
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlButton(
                    icon = if (options.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    isActive = options.isMicMuted,
                    activeColor = Color(0xFFEF4444),
                    defaultColor = Color.White.copy(alpha = 0.2f),
                    contentDescription = "Mute Mikrofon",
                    onClick = { onUpdateOptions { it.copy(isMicMuted = !it.isMicMuted) } }
                )
                CallControlButton(
                    icon = if (options.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    isActive = !options.isCameraOn,
                    activeColor = Color(0xFFEF4444),
                    defaultColor = Color.White.copy(alpha = 0.2f),
                    contentDescription = "Kamera On/Off",
                    onClick = { onUpdateOptions { it.copy(isCameraOn = !it.isCameraOn) } }
                )
                CallControlButton(
                    icon = Icons.Default.FlipCameraIos,
                    isActive = false,
                    activeColor = Color(0xFF00E5FF),
                    defaultColor = Color.White.copy(alpha = 0.2f),
                    contentDescription = "Putar Kamera",
                    onClick = { onUpdateOptions { it.copy(isFrontCamera = !it.isFrontCamera) } }
                )
                CallControlButton(
                    icon = if (options.isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                    isActive = options.isScreenSharing,
                    activeColor = Color(0xFF8B5CF6),
                    defaultColor = Color.White.copy(alpha = 0.2f),
                    contentDescription = "Berbagi Layar",
                    onClick = { onUpdateOptions { it.copy(isScreenSharing = !it.isScreenSharing) } }
                )
                CallControlButton(
                    icon = Icons.Default.AutoAwesome,
                    isActive = isEffectsMenuOpen,
                    activeColor = Color(0xFF00E5FF),
                    defaultColor = Color.White.copy(alpha = 0.2f),
                    contentDescription = "Menu Efek Kamera",
                    onClick = { isEffectsMenuOpen = !isEffectsMenuOpen }
                )
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Akhiri Panggilan",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    defaultColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else defaultColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun FilterColorShaderOverlay(filter: ColorFilterType) {
    when (filter) {
        ColorFilterType.ORIGINAL -> {}
        ColorFilterType.BEAUTY_GLOW -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFB6C1).copy(alpha = 0.15f))
            )
        }
        ColorFilterType.CYBER_NEON -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.20f),
                                Color(0xFF7C4DFF).copy(alpha = 0.25f)
                            )
                        )
                    )
            )
        }
        ColorFilterType.MONO_NOIR -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }
        ColorFilterType.VINTAGE_WARM -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD4A373).copy(alpha = 0.22f))
            )
        }
        ColorFilterType.GOLDEN_HOUR -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFD166).copy(alpha = 0.30f),
                                Color(0xFFF77F00).copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        ColorFilterType.EMERALD_MINT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06D6A0).copy(alpha = 0.18f))
            )
        }
    }
}

@Composable
fun ArFaceEffectOverlay(effect: ArFaceEffect) {
    when (effect) {
        ArFaceEffect.NONE -> {}
        ArFaceEffect.CYBER_GLASSES -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.35f
                drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(cx - 120f, cy - 25f),
                    size = androidx.compose.ui.geometry.Size(240f, 50f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f, 15f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                )
                drawRoundRect(
                    color = Color(0xFF7C4DFF).copy(alpha = 0.4f),
                    topLeft = Offset(cx - 115f, cy - 20f),
                    size = androidx.compose.ui.geometry.Size(230f, 40f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            }
        }
        ArFaceEffect.NEON_AURA -> {
            val infiniteTransition = rememberInfiniteTransition(label = "NeonAura")
            val auraRadius by infiniteTransition.animateFloat(
                initialValue = 180f,
                targetValue = 240f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "AuraRadius"
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.35f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.4f),
                            Color(0xFF7C4DFF).copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = auraRadius
                    ),
                    center = Offset(cx, cy),
                    radius = auraRadius
                )
            }
        }
        ArFaceEffect.STAR_BURST -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.32f
                val stars = listOf(
                    Offset(cx - 90f, cy - 80f),
                    Offset(cx + 90f, cy - 80f),
                    Offset(cx, cy - 100f),
                    Offset(cx - 130f, cy - 20f),
                    Offset(cx + 130f, cy - 20f)
                )
                stars.forEach { pos ->
                    drawCircle(color = Color(0xFFFFD166), radius = 8f, center = pos)
                    drawCircle(color = Color.White, radius = 4f, center = pos)
                }
            }
        }
        ArFaceEffect.FIRE_CROWN -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.24f
                val crownPath = Path().apply {
                    moveTo(cx - 90f, cy + 30f)
                    lineTo(cx - 90f, cy - 30f)
                    lineTo(cx - 45f, cy)
                    lineTo(cx, cy - 50f)
                    lineTo(cx + 45f, cy)
                    lineTo(cx + 90f, cy - 30f)
                    lineTo(cx + 90f, cy + 30f)
                    close()
                }
                drawPath(crownPath, color = Color(0xFFFF9F1C))
                drawCircle(color = Color(0xFFFF4040), radius = 10f, center = Offset(cx, cy - 50f))
            }
        }
        ArFaceEffect.LASER_EYES -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.35f
                drawLine(
                    color = Color(0xFFFF0055),
                    start = Offset(cx - 45f, cy),
                    end = Offset(cx - 180f, size.height),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFF0055),
                    start = Offset(cx + 45f, cy),
                    end = Offset(cx + 180f, size.height),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color.White, radius = 8f, center = Offset(cx - 45f, cy))
                drawCircle(color = Color.White, radius = 8f, center = Offset(cx + 45f, cy))
            }
        }
        ArFaceEffect.CLOWN_FACE -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.38f
                drawCircle(color = Color(0xFFFF2A2A), radius = 26f, center = Offset(cx, cy))
                drawCircle(color = Color.White, radius = 6f, center = Offset(cx - 8f, cy - 8f))
                drawArc(
                    color = Color(0xFFFF2A2A),
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(cx - 70f, cy + 10f),
                    size = androidx.compose.ui.geometry.Size(140f, 60f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }
        }
        ArFaceEffect.ANGRY_DEVIL -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.24f
                val leftHorn = Path().apply {
                    moveTo(cx - 70f, cy + 20f)
                    quadraticBezierTo(cx - 100f, cy - 40f, cx - 120f, cy - 80f)
                    quadraticBezierTo(cx - 60f, cy - 40f, cx - 40f, cy + 10f)
                    close()
                }
                drawPath(leftHorn, color = Color(0xFFFF2222))
                val rightHorn = Path().apply {
                    moveTo(cx + 70f, cy + 20f)
                    quadraticBezierTo(cx + 100f, cy - 40f, cx + 120f, cy - 80f)
                    quadraticBezierTo(cx + 60f, cy - 40f, cx + 40f, cy + 10f)
                    close()
                }
                drawPath(rightHorn, color = Color(0xFFFF2222))
            }
        }
        ArFaceEffect.CAT_EARS -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.22f
                val pathLeft = Path().apply {
                    moveTo(cx - 110f, cy)
                    lineTo(cx - 70f, cy - 80f)
                    lineTo(cx - 30f, cy)
                    close()
                }
                drawPath(pathLeft, color = Color(0xFFFF2A85))
                val pathRight = Path().apply {
                    moveTo(cx + 30f, cy)
                    lineTo(cx + 70f, cy - 80f)
                    lineTo(cx + 110f, cy)
                    close()
                }
                drawPath(pathRight, color = Color(0xFFFF2A85))
            }
        }
        ArFaceEffect.STRAWBERRY_HAT -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.20f
                val berry = Path().apply {
                    moveTo(cx, cy + 50f)
                    lineTo(cx - 70f, cy - 50f)
                    lineTo(cx + 70f, cy - 50f)
                    close()
                }
                drawPath(berry, color = Color(0xFFFF1E40))
                drawCircle(color = Color(0xFF22BB33), radius = 18f, center = Offset(cx - 30f, cy - 50f))
                drawCircle(color = Color(0xFF22BB33), radius = 22f, center = Offset(cx, cy - 55f))
                drawCircle(color = Color(0xFF22BB33), radius = 18f, center = Offset(cx + 30f, cy - 50f))
            }
        }
        else -> {}
    }
}
