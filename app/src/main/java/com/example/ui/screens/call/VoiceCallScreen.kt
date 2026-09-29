package com.example.ui.screens.call

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CallNetworkMode
import com.example.data.models.VideoCallOptions
import com.example.data.models.VoiceChangerEffect
import com.example.ui.components.FloatingTorchButton
import com.example.ui.viewmodels.ActiveCallState

@Composable
fun VoiceCallScreen(
    callState: ActiveCallState,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onUpdateOptions: ((VideoCallOptions) -> VideoCallOptions) -> Unit,
    onEndCall: () -> Unit,
    onSwitchToGsm: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onEndCall)
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVoiceChangerDrawerOpen by remember { mutableStateOf(false) }

    val activeVoiceChanger = callState.callOptions.voiceChanger
    val durationText = remember(callState.durationSeconds) {
        val minutes = callState.durationSeconds / 60
        val seconds = callState.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "VoicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (activeVoiceChanger != VoiceChangerEffect.NORMAL) 1.22f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (activeVoiceChanger == VoiceChangerEffect.CHIPMUNK) 600 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090D16),
                        Color(0xFF111827),
                        Color(0xFF1E293B)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Surface(
                    color = when (callState.networkMode) {
                        CallNetworkMode.VOIP_WIFI -> Color(0xFF10B981).copy(alpha = 0.2f)
                        CallNetworkMode.VOIP_CELLULAR -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                        CallNetworkMode.GSM_OPERATOR -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (callState.networkMode) {
                            CallNetworkMode.VOIP_WIFI -> Color(0xFF10B981)
                            CallNetworkMode.VOIP_CELLULAR -> Color(0xFF00E5FF)
                            CallNetworkMode.GSM_OPERATOR -> Color(0xFFF59E0B)
                        }
                    )
                ) {
                    Text(
                        text = when (callState.networkMode) {
                            CallNetworkMode.VOIP_WIFI -> "VoIP Wi-Fi HD (Opus Codec)"
                            CallNetworkMode.VOIP_CELLULAR -> "VoIP 5G Data Seluler"
                            CallNetworkMode.GSM_OPERATOR -> "Panggilan Operator GSM Fallback"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = callState.contactName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = callState.phoneNumber,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = durationText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )

                if (activeVoiceChanger != VoiceChangerEffect.NORMAL) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFF2A85).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A85))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = activeVoiceChanger.emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pengubah Suara: ${activeVoiceChanger.label} Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            if (activeVoiceChanger != VoiceChangerEffect.NORMAL) Color(0xFFFF2A85).copy(alpha = 0.25f)
                            else Color(0xFF00E5FF).copy(alpha = 0.15f)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale * 0.95f)
                        .clip(CircleShape)
                        .background(Color(0xFF7C4DFF).copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callState.contactName.take(1),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF090D16)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = isVoiceChangerDrawerOpen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pengubah Suara Real-Time",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                                IconButton(onClick = { isVoiceChangerDrawerOpen = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                                }
                            }
                            Text(
                                text = "Pilih modulasi suara yang didengar lawan bicara:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(VoiceChangerEffect.values()) { voice ->
                                    val isSelected = activeVoiceChanger == voice
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.3f) else Color(0xFF1E293B),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
                                        modifier = Modifier.clickable {
                                            onUpdateOptions { it.copy(voiceChanger = voice) }
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = voice.emoji, fontSize = 22.sp)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = voice.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFF00E5FF) else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerOn) Color(0xFF090D16) else Color.White
                        )
                    }
                    IconButton(
                        onClick = { isVoiceChangerDrawerOpen = !isVoiceChangerDrawerOpen },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeVoiceChanger != VoiceChangerEffect.NORMAL) Color(0xFFFF2A85)
                                else Color.White.copy(alpha = 0.15f)
                            )
                            .testTag("voice_changer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Pengubah Suara",
                            tint = Color.White
                        )
                    }
                    FloatingTorchButton(
                        isTorchOn = isTorchOn,
                        onToggle = onToggleTorch,
                        modifier = Modifier.size(50.dp)
                    )
                    IconButton(
                        onClick = onSwitchToGsm,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFFF59E0B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Alihkan ke GSM Telepon",
                            tint = Color(0xFFF59E0B)
                        )
                    }
                }

                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("end_voice_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Akhiri Panggilan",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
