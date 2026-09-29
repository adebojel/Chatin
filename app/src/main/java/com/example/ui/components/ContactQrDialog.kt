package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.UserAccount

enum class QrTab {
    MY_CODE,
    SCAN_CODE
}

@Composable
fun ContactQrDialog(
    user: UserAccount?,
    onContactScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(QrTab.MY_CODE) }
    var scanManualInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
            color = Color(0xFF0D1424),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Kode QR Kontak",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tukar Kontak Cepat & Instan",
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
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF162035),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == QrTab.MY_CODE) Color(0xFF00E5FF) else Color.Transparent)
                                .clickable { selectedTab = QrTab.MY_CODE }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = if (selectedTab == QrTab.MY_CODE) Color(0xFF090D16) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kode QR Saya",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == QrTab.MY_CODE) Color(0xFF090D16) else Color.White
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == QrTab.SCAN_CODE) Color(0xFF00E5FF) else Color.Transparent)
                                .clickable { selectedTab = QrTab.SCAN_CODE }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = if (selectedTab == QrTab.SCAN_CODE) Color(0xFF090D16) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pindai Kode QR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == QrTab.SCAN_CODE) Color(0xFF090D16) else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedTab == QrTab.MY_CODE) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(240.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContactQrMatrixView()
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user?.displayName?.take(1)?.uppercase() ?: "C",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color(0xFF090D16)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = user?.displayName ?: "Pengguna Chatin",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF090D16)
                            )
                            Text(
                                text = user?.username ?: "@user",
                                fontSize = 11.sp,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Arahkan kamera teman Anda ke kode ini untuk langsung terhubung dan mulai berkirim pesan terenkripsi di Chatin.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val link = "chatin://contact/${user?.username?.removePrefix("@") ?: "user"}"
                                clipboardManager.setText(AnnotatedString(link))
                                Toast.makeText(context, "Tautan profil disalin: $link", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283D)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Tautan", color = Color(0xFF00E5FF), fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                Toast.makeText(context, "Membagikan Kode QR profil Chatin...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF090D16), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan QR", color = Color(0xFF090D16), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Scanner Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Arahkan kamera ke Kode QR teman",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pindai kode QR atau ketik username/nomor ponsel untuk bertukar kontak:",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF151F33))
                                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraPreviewView(isFrontCamera = false)
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = scanManualInput,
                            onValueChange = { scanManualInput = it },
                            placeholder = { Text("Ketik nomor telepon atau @username...", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (scanManualInput.isNotBlank()) {
                                    onContactScanned(scanManualInput.trim())
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Silakan masukkan username atau nomor kontak", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF090D16))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hubungkan Kontak & Mulai Obrolan", color = Color(0xFF090D16), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactQrMatrixView() {
    Canvas(modifier = Modifier.size(150.dp)) {
        val squareCount = 8
        val cellSize = size.width / squareCount

        // 3 Corner Finder Patterns
        // Top-Left
        drawRoundRect(
            color = Color(0xFF090D16),
            topLeft = Offset(0f, 0f),
            size = Size(cellSize * 2.2f, cellSize * 2.2f),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 6f)
        )
        drawCircle(
            color = Color(0xFF090D16),
            center = Offset(cellSize * 1.1f, cellSize * 1.1f),
            radius = cellSize * 0.45f
        )
        // Top-Right
        drawRoundRect(
            color = Color(0xFF090D16),
            topLeft = Offset(size.width - cellSize * 2.2f, 0f),
            size = Size(cellSize * 2.2f, cellSize * 2.2f),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 6f)
        )
        drawCircle(
            color = Color(0xFF090D16),
            center = Offset(size.width - cellSize * 1.1f, cellSize * 1.1f),
            radius = cellSize * 0.45f
        )
        // Bottom-Left
        drawRoundRect(
            color = Color(0xFF090D16),
            topLeft = Offset(0f, size.height - cellSize * 2.2f),
            size = Size(cellSize * 2.2f, cellSize * 2.2f),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 6f)
        )
        drawCircle(
            color = Color(0xFF090D16),
            center = Offset(cellSize * 1.1f, size.height - cellSize * 1.1f),
            radius = cellSize * 0.45f
        )

        // Data dots
        for (i in 0..squareCount) {
            for (j in 0..squareCount) {
                if ((i * 3 + j * 5) % 2 == 0) {
                    val px = i * cellSize + cellSize * 0.25f
                    val py = j * cellSize + cellSize * 0.25f
                    val isCenter = px in (cellSize * 2.5f)..(cellSize * 5.5f) && py in (cellSize * 2.5f)..(cellSize * 5.5f)
                    if (!isCenter) {
                        drawRoundRect(
                            color = Color(0xFF090D16),
                            topLeft = Offset(px, py),
                            size = Size(cellSize * 0.5f, cellSize * 0.5f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }
    }
}
