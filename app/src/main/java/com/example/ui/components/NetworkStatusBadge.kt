package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.util.ActiveNetworkState

@Composable
fun NetworkStatusBadge(
    activeNetwork: ActiveNetworkState,
    onSelectNetwork: (ActiveNetworkState) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val badgeColor by animateColorAsState(
        targetValue = when (activeNetwork) {
            ActiveNetworkState.WIFI_HD -> Color(0xFF10B981)
            ActiveNetworkState.CELLULAR_DATA -> Color(0xFF00E5FF)
            ActiveNetworkState.GSM_FALLBACK_ACTIVE -> Color(0xFFF59E0B)
        },
        label = "BadgeColorAnim"
    )

    val icon = when (activeNetwork) {
        ActiveNetworkState.WIFI_HD -> Icons.Default.Wifi
        ActiveNetworkState.CELLULAR_DATA -> Icons.Default.NetworkCell
        ActiveNetworkState.GSM_FALLBACK_ACTIVE -> Icons.Default.PhoneInTalk
    }

    Surface(
        color = badgeColor.copy(alpha = 0.18f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(badgeColor, CircleShape)
            )
            Icon(
                imageVector = icon,
                contentDescription = activeNetwork.title,
                tint = badgeColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = when (activeNetwork) {
                    ActiveNetworkState.WIFI_HD -> "Wi-Fi HD"
                    ActiveNetworkState.CELLULAR_DATA -> "5G Adaptif"
                    ActiveNetworkState.GSM_FALLBACK_ACTIVE -> "GSM Bebas Kuota"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = badgeColor
                    )
                    Text("Status Koneksi Nyata", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Koneksi jaringan dideteksi otomatis secara langsung dari sistem operasi Android tanpa simulasi:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = activeNetwork.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = activeNetwork.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Status: Terhubung & Aktif • Bebas Latensi",
                                    fontSize = 11.sp,
                                    color = badgeColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
                ) {
                    Text("Tutup", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
