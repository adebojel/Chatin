package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.ChatConversation
import com.example.data.models.HiddenChatSecurity
import com.example.data.models.LockType

@Composable
fun HiddenChatSecurityDialog(
    securityState: HiddenChatSecurity,
    hiddenConversations: List<ChatConversation>,
    allConversations: List<ChatConversation>,
    isUnlocked: Boolean,
    onVerifyCode: (String) -> Boolean,
    onTriggerBiometricAuth: ((onSuccess: () -> Unit) -> Unit)? = null,
    onBiometricUnlockSuccess: () -> Unit = {},
    onUpdateSecurityConfig: (LockType, String) -> Unit,
    onToggleHideChat: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onLockAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(securityState.lockType) }
    var enteredCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConfiguringLock by remember { mutableStateOf(false) }
    var newCodeInput by remember { mutableStateOf("") }
    var showPasswordText by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFFFF2A85).copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            color = Color(0xFF0F121E),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF2A85).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF2A85),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ruang Obrolan Rahasia",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Diakses lewat Ketuk 2x Logo 'chatin'",
                                fontSize = 11.sp,
                                color = Color(0xFFFF2A85)
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

                Spacer(modifier = Modifier.height(16.dp))

                if (!isUnlocked) {
                    // AUTHENTICATION SCREEN
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Otentikasi Kunci Pengaman",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gunakan Sensor Biometrik Bawaan OS atau PIN/Pola",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Lock Type Selector
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E2438),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                LockType.values().forEach { type ->
                                    val isSelected = selectedTab == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFFFF2A85) else Color.Transparent)
                                            .clickable {
                                                selectedTab = type
                                                enteredCode = ""
                                                errorMessage = null
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        when (selectedTab) {
                            LockType.BIOMETRIC -> {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF2A85).copy(alpha = 0.2f))
                                        .border(2.dp, Color(0xFFFF2A85), CircleShape)
                                        .clickable {
                                            if (onTriggerBiometricAuth != null) {
                                                onTriggerBiometricAuth {
                                                    onBiometricUnlockSuccess()
                                                }
                                            } else {
                                                onBiometricUnlockSuccess()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Pindai Sidik Jari Asli",
                                        tint = Color(0xFFFF2A85),
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        if (onTriggerBiometricAuth != null) {
                                            onTriggerBiometricAuth {
                                                onBiometricUnlockSuccess()
                                            }
                                        } else {
                                            onBiometricUnlockSuccess()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A85)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buka dengan Biometrik Sistem OS", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            LockType.PIN -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    for (i in 0 until 4) {
                                        val isFilled = enteredCode.length > i
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(if (isFilled) Color(0xFFFF2A85) else Color.White.copy(alpha = 0.2f))
                                                .border(1.5.dp, Color(0xFFFF2A85), CircleShape)
                                        )
                                    }
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val keys = listOf(
                                        listOf("1", "2", "3"),
                                        listOf("4", "5", "6"),
                                        listOf("7", "8", "9"),
                                        listOf("C", "0", "⌫")
                                    )
                                    keys.forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            row.forEach { key ->
                                                Surface(
                                                    modifier = Modifier
                                                        .size(58.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            when (key) {
                                                                "C" -> enteredCode = ""
                                                                "⌫" -> if (enteredCode.isNotEmpty()) enteredCode = enteredCode.dropLast(1)
                                                                else -> {
                                                                    if (enteredCode.length < 4) {
                                                                        enteredCode += key
                                                                        if (enteredCode.length == 4) {
                                                                            val ok = onVerifyCode(enteredCode)
                                                                            if (!ok) errorMessage = "PIN salah! Coba default: 1234"
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    shape = CircleShape,
                                                    color = Color(0xFF1E2438),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(text = key, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            LockType.PATTERN -> {
                                Text(
                                    text = "Hubungkan minimal 4 titik pola",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    for (r in 0..2) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                            for (c in 0..2) {
                                                val pointIndex = (r * 3 + c + 1).toString()
                                                val isToggled = enteredCode.contains(pointIndex)
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isToggled) Color(0xFFFF2A85) else Color(0xFF1E2438))
                                                        .border(2.dp, if (isToggled) Color.White else Color(0xFFFF2A85).copy(alpha = 0.4f), CircleShape)
                                                        .clickable {
                                                            if (!enteredCode.contains(pointIndex)) {
                                                                enteredCode += pointIndex
                                                                if (enteredCode.length >= 4) {
                                                                    val ok = onVerifyCode(enteredCode)
                                                                    if (!ok) errorMessage = "Pola salah! Coba pola 1234"
                                                                }
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextButton(onClick = { enteredCode = "" }) {
                                        Text("Ulangi Pola", color = Color.White.copy(alpha = 0.7f))
                                    }
                                    TextButton(onClick = {
                                        enteredCode = "1234"
                                        onVerifyCode("1234")
                                    }) {
                                        Text("Pola Default: 1234", color = Color(0xFF00E5FF))
                                    }
                                }
                            }

                            LockType.PASSWORD -> {
                                OutlinedTextField(
                                    value = enteredCode,
                                    onValueChange = { enteredCode = it },
                                    label = { Text("Kata Sandi Rahasia") },
                                    singleLine = true,
                                    visualTransformation = if (showPasswordText) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPasswordText = !showPasswordText }) {
                                            Icon(
                                                imageVector = if (showPasswordText) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF2A85),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        val ok = onVerifyCode(enteredCode)
                                        if (!ok) errorMessage = "Kata sandi salah! Coba: 1234"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A85)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    Text("Buka Obrolan Rahasia", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = {
                            enteredCode = "1234"
                            onVerifyCode("1234")
                        }) {
                            Text(
                                text = "Buka dengan Kode Default (1234)",
                                fontSize = 11.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                } else {
                    // UNLOCKED: VAULT OF HIDDEN CHATS
                    if (isConfiguringLock) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                text = "Ubah Kunci Pengaman Obrolan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Pilih Jenis Kunci Baru:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LockType.values().forEach { t ->
                                    val isCur = selectedTab == t
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCur) Color(0xFFFF2A85) else Color(0xFF1E2438),
                                        modifier = Modifier.clickable { selectedTab = t }
                                    ) {
                                        Text(
                                            text = t.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = newCodeInput,
                                onValueChange = { newCodeInput = it },
                                label = { Text("Kode / Sandi Baru") },
                                placeholder = { Text("cth: 5678 atau SandiKu123") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF2A85),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        if (newCodeInput.isNotBlank()) {
                                            onUpdateSecurityConfig(selectedTab, newCodeInput)
                                            isConfiguringLock = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2A85)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Simpan Perubahan", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { isConfiguringLock = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Batal", color = Color.White)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OBROLAN TERSEMBUNYI AKTIF (${hiddenConversations.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF2A85)
                            )
                            Row {
                                TextButton(onClick = { isConfiguringLock = true }) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ganti Sandi", fontSize = 11.sp, color = Color(0xFF00E5FF))
                                }
                                TextButton(onClick = onLockAgain) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kunci Sekarang", fontSize = 11.sp, color = Color(0xFFEF4444))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (hiddenConversations.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada obrolan yang disembunyikan.\nPilih obrolan dari beranda lalu sembunyikan ke ruang rahasia ini.",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(hiddenConversations) { conv ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF192036),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A85).copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onOpenChat(conv.id)
                                                onDismiss()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(conv.contact.avatarColorHex)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = conv.contact.name.take(1),
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = conv.contact.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = conv.lastMessage,
                                                        fontSize = 12.sp,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { onToggleHideChat(conv.id) },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.1f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Visibility,
                                                    contentDescription = "Tampilkan kembali ke Beranda",
                                                    tint = Color(0xFF00E5FF),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
