package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.UserAccount

enum class ChangePhoneStep {
    INPUT_PHONE,
    INPUT_OTP
}

/**
 * Dialog Alur Ubah Nomor Telepon Asli & Verifikasi Kode OTP 2-Tahap
 * Tahap 1: Input nomor ponsel baru -> Kirim string kode OTP 6 angka ke nomor baru
 * Tahap 2: Input kode OTP verifikasi untuk memperbarui record database secara permanen
 */
@Composable
fun ChangePhoneNumberDialog(
    user: UserAccount?,
    onRequestOtp: (newPhone: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onVerifyOtp: (newPhone: String, otpCode: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(ChangePhoneStep.INPUT_PHONE) }
    var newPhoneInput by remember { mutableStateOf("+62 ") }
    var otpInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            color = Color(0xFF0D1424),
            shadowElevation = 24.dp
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
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ubah Nomor Telepon",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Verifikasi OTP 2-Tahap Asli",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981)
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

                Spacer(modifier = Modifier.height(18.dp))

                // Current Phone Info Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF162035),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Nomor Telepon Saat Ini:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(user?.phoneNumber?.ifBlank { "-" } ?: "-", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (currentStep == ChangePhoneStep.INPUT_PHONE) {
                    // TAHAP 1: INPUT NOMOR PONSEL BARU
                    Text(
                        text = "Tahap 1: Masukkan Nomor Baru",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sistem akan mengirimkan kode verifikasi OTP 6-angka ke nomor baru tersebut.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newPhoneInput,
                        onValueChange = { newPhoneInput = it },
                        label = { Text("Nomor Telepon Baru") },
                        placeholder = { Text("+62 8xx-xxxx-xxxx") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF10B981))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_phone_input_field")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val clean = newPhoneInput.trim()
                            if (clean.length < 9) {
                                errorMessage = "Masukkan nomor telepon yang valid"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            onRequestOtp(clean) { success, message ->
                                isLoading = false
                                if (success) {
                                    infoMessage = message
                                    currentStep = ChangePhoneStep.INPUT_OTP
                                } else {
                                    errorMessage = message
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("request_otp_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF090D16))
                        } else {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFF090D16))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim Kode OTP 6-Digit", color = Color(0xFF090D16), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    // TAHAP 2: INPUT KODE VERIFIKASI OTP
                    Text(
                        text = "Tahap 2: Verifikasi Kode OTP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = infoMessage ?: "Masukkan 6-digit kode OTP yang dikirim ke $newPhoneInput",
                        fontSize = 12.sp,
                        color = Color(0xFF10B981),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val extractedOtp = remember(infoMessage) {
                        infoMessage?.let { msg ->
                            Regex("\\b\\d{6}\\b").find(msg)?.value
                        }
                    }

                    if (extractedOtp != null && otpInput.isEmpty()) {
                        AssistChip(
                            onClick = { otpInput = extractedOtp },
                            label = { Text("Tempel Kode OTP: $extractedOtp", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                otpInput = it
                            }
                        },
                        label = { Text("Kode Verifikasi 6 Angka") },
                        placeholder = { Text("000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color(0xFF00E5FF),
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(0.85f).testTag("verify_phone_otp_input")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (otpInput.length < 6) {
                                errorMessage = "Masukkan 6-digit kode OTP secara lengkap"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            onVerifyOtp(newPhoneInput.trim(), otpInput.trim()) { success, msg ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Nomor telepon berhasil diperbarui ke $newPhoneInput!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_phone_otp_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF090D16))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF090D16))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifikasi & Perbarui Nomor", color = Color(0xFF090D16), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = { currentStep = ChangePhoneStep.INPUT_PHONE }) {
                        Text("Ubah Nomor Ponsel Kembali", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}
