package com.example.ui.screens.calls_history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallsHistoryScreen(
    callHistory: List<CallRecord>,
    onStartCall: (name: String, number: String, type: CallMediaType, forceGsm: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Semua") }
    val filters = listOf("Semua", "Tak Terjawab", "VoIP HD", "Telepon GSM")

    val filteredCalls = remember(callHistory, selectedFilter) {
        callHistory.filter { call ->
            when (selectedFilter) {
                "Tak Terjawab" -> call.direction == CallDirection.MISSED
                "VoIP HD" -> call.networkMode == CallNetworkMode.VOIP_WIFI || call.networkMode == CallNetworkMode.VOIP_CELLULAR
                "Telepon GSM" -> call.networkMode == CallNetworkMode.GSM_OPERATOR
                else -> true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Riwayat Panggilan",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "VoIP HD, Panggilan Video, & Fallback Operator GSM",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (filteredCalls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhoneMissed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Belum ada riwayat panggilan",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCalls, key = { it.id }) { call ->
                    CallHistoryItem(
                        call = call,
                        onCallVoip = {
                            onStartCall(call.contactName, call.phoneNumber, call.mediaType, false)
                        },
                        onCallGsm = {
                            onStartCall(call.contactName, call.phoneNumber, CallMediaType.AUDIO, true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(
    call: CallRecord,
    onCallVoip: () -> Unit,
    onCallGsm: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(call.timestamp) { timeFormat.format(Date(call.timestamp)) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(call.avatarColorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = call.contactName.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (call.direction == CallDirection.MISSED) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (call.direction) {
                        CallDirection.INCOMING -> {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                contentDescription = "Masuk",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        CallDirection.OUTGOING -> {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallMade,
                                contentDescription = "Keluar",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        CallDirection.MISSED -> {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallMissed,
                                contentDescription = "Tak Terjawab",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    val (netName, netColor) = when (call.networkMode) {
                        CallNetworkMode.VOIP_WIFI -> "Wi-Fi HD" to Color(0xFF10B981)
                        CallNetworkMode.VOIP_CELLULAR -> "5G Seluler" to Color(0xFF00E5FF)
                        CallNetworkMode.GSM_OPERATOR -> "GSM Fallback" to Color(0xFFF59E0B)
                    }
                    Text(
                        text = "$netName • $formattedDate",
                        fontSize = 11.sp,
                        color = netColor
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onCallVoip,
                    modifier = Modifier.testTag("call_again_voip_${call.id}")
                ) {
                    Icon(
                        imageVector = if (call.mediaType == CallMediaType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Panggil via VoIP",
                        tint = Color(0xFF00E5FF)
                    )
                }
                IconButton(
                    onClick = onCallGsm,
                    modifier = Modifier.testTag("call_again_gsm_${call.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "Panggil via Operator GSM",
                        tint = Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}
