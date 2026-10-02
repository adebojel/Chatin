package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.DefaultAppHelper

@Composable
fun DefaultAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isDefaultSms by remember { mutableStateOf(DefaultAppHelper.isDefaultSmsApp(context)) }
    var isDefaultDialer by remember { mutableStateOf(DefaultAppHelper.isDefaultDialerApp(context)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            color = Color(0xFF0D1424),
            shadowElevation = 24.dp
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Aplikasi Resmi & Default",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Jadikan Chatin Aplikasi Utama Perangkat",
                                fontSize = 11.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Card 1: Default SMS App
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF162035),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDefaultSms) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = if (isDefaultSms) Color(0xFF10B981) else Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Aplikasi SMS Default",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isDefaultSms) "Status: Sudah Aktif Sebagai Default" else "Status: Belum Default",
                                        fontSize = 11.sp,
                                        color = if (isDefaultSms) Color(0xFF10B981) else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (isDefaultSms) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Mengizinkan Chatin menerima dan mengirim SMS operator seluler secara langsung di ponsel Android Anda.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = DefaultAppHelper.createSetDefaultSmsIntent(context)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Membuka setelan SMS default: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDefaultSms) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF00E5FF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("set_default_sms_button")
                        ) {
                            Icon(
                                imageVector = if (isDefaultSms) Icons.Default.Check else Icons.Default.SettingsApplications,
                                contentDescription = null,
                                tint = if (isDefaultSms) Color(0xFF10B981) else Color(0xFF090D16)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDefaultSms) "Ubah Pilihan SMS Default" else "Jadikan Aplikasi SMS Default",
                                fontWeight = FontWeight.Bold,
                                color = if (isDefaultSms) Color(0xFF10B981) else Color(0xFF090D16),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 2: Default Phone Dialer App
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF162035),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDefaultDialer) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = if (isDefaultDialer) Color(0xFF10B981) else Color(0xFF7C4DFF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Aplikasi Telepon & Panggilan Default",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isDefaultDialer) "Status: Sudah Aktif Sebagai Default" else "Status: Belum Default",
                                        fontSize = 11.sp,
                                        color = if (isDefaultDialer) Color(0xFF10B981) else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (isDefaultDialer) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Menjadikan dialer Chatin sebagai penangan panggilan suara & nomor darurat utama di sistem telepon.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = DefaultAppHelper.createSetDefaultDialerIntent(context)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Membuka setelan dialer default: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDefaultDialer) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF7C4DFF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("set_default_dialer_button")
                        ) {
                            Icon(
                                imageVector = if (isDefaultDialer) Icons.Default.Check else Icons.Default.Call,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDefaultDialer) "Ubah Pilihan Dialer Default" else "Jadikan Aplikasi Telepon Default",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 3: Pengaturan Aplikasi Default Sistem Android Langsung
                OutlinedButton(
                    onClick = {
                        DefaultAppHelper.openSystemDefaultAppsSettings(context)
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buka Setelan Default Sistem Android",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
