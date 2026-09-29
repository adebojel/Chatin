package com.example.ui.screens.dialer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CallMediaType

@Composable
fun DialerScreen(
    onDialGsm: (String) -> Unit,
    onDialVoip: (String, CallMediaType) -> Unit,
    onSendSms: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dialNumber by remember { mutableStateOf("") }
    val keypadButtons = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Dialer GSM Seluler & SMS Operator (Bebas Internet)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (dialNumber.isEmpty()) "Ketik nomor telepon..." else dialNumber,
                    fontSize = if (dialNumber.length > 12) 22.sp else 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dialNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialer_display_text")
                )
                if (dialNumber.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (dialNumber.isNotEmpty()) {
                                dialNumber = dialNumber.dropLast(1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Hapus Angka",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            keypadButtons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (digit, letters) ->
                        KeypadButton(
                            digit = digit,
                            letters = letters,
                            onClick = { dialNumber += digit },
                            onLongClick = {
                                if (digit == "0") dialNumber += "+"
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SMS Fallback
            IconButton(
                onClick = {
                    if (dialNumber.isNotEmpty()) onSendSms(dialNumber)
                },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                    .testTag("dialer_sms_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = "Kirim SMS Fallback",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(24.dp)
                )
            }

            // GSM Operator Call Button (Main)
            IconButton(
                onClick = {
                    if (dialNumber.isNotEmpty()) onDialGsm(dialNumber)
                },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .testTag("dialer_gsm_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = "Panggil via GSM Operator",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // VoIP HD Video Call
            IconButton(
                onClick = {
                    if (dialNumber.isNotEmpty()) onDialVoip(dialNumber, CallMediaType.VIDEO)
                },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                    .testTag("dialer_voip_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Panggil via WebRTC HD",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun KeypadButton(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("keypad_$digit")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
