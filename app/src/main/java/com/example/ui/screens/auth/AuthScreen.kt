package com.example.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.AuthStep
import com.example.data.models.AuthUiState
import kotlinx.coroutines.delay

/**
 * Layar Autentikasi Alur Nomor Telepon WhatsApp-Style:
 * 1. Input Nomor Telepon dengan Konfirmasi Nomor
 * 2. Verifikasi Kode OTP 6-Digit Asli
 * 3. Lengkapi Info Profil (Nama & Foto)
 */
@Composable
fun AuthScreen(
    authUiState: AuthUiState,
    onRequestPhoneLogin: (phoneNumber: String) -> Unit,
    onVerifyOtp: (code: String) -> Unit,
    onResendOtp: () -> Unit,
    onCompleteProfile: (displayName: String, bio: String, avatarUrl: String) -> Unit,
    onSwitchStep: (AuthStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0B111F)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F172A),
                            Color(0xFF0B111F),
                            Color(0xFF060910)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Brand Header Chatin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Chatin Logo",
                            tint = Color(0xFF090D16),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "chatin",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                AnimatedContent(
                    targetState = authUiState.currentStep,
                    label = "auth_whatsapp_step"
                ) { step ->
                    when (step) {
                        AuthStep.LOGIN, AuthStep.REGISTER -> {
                            WhatsAppPhoneInputView(
                                errorMessage = authUiState.errorMessage,
                                onConfirmPhoneNumber = { fullNumber ->
                                    onRequestPhoneLogin(fullNumber)
                                }
                            )
                        }
                        AuthStep.VERIFY_OTP -> {
                            WhatsAppOtpVerifyView(
                                phoneNumber = authUiState.pendingPhoneNumber,
                                errorMessage = authUiState.errorMessage,
                                onVerify = onVerifyOtp,
                                onResend = onResendOtp,
                                onBackToPhoneInput = { onSwitchStep(AuthStep.LOGIN) }
                            )
                        }
                        AuthStep.PROFILE_SETUP -> {
                            WhatsAppProfileSetupView(
                                phoneNumber = authUiState.pendingPhoneNumber,
                                onComplete = onCompleteProfile
                            )
                        }
                        AuthStep.AUTHENTICATED -> {
                            // Layar Utama diatur oleh MainAppScaffold
                        }
                    }
                }
            }
        }
    }
}

/**
 * TAHAP 1: INPUT NOMOR TELEPON (PERSIS WHATSAPP)
 */
@Composable
fun WhatsAppPhoneInputView(
    errorMessage: String?,
    onConfirmPhoneNumber: (fullNumber: String) -> Unit
) {
    var rawPhoneInput by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf<String?>(null) }

    val formattedNumber = remember(rawPhoneInput) {
        val clean = rawPhoneInput.trim().replace("[^0-9]".toRegex(), "")
        val localPart = if (clean.startsWith("62")) {
            clean.removePrefix("62")
        } else if (clean.startsWith("0")) {
            clean.removePrefix("0")
        } else {
            clean
        }
        "+62 $localPart"
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Konfirmasi Nomor Telepon",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Kami akan memverifikasi nomor telepon Anda:",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = formattedNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Apakah nomor ini sudah benar, atau Anda ingin mengubahnya?",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmPhoneNumber(formattedNumber.replace(" ", ""))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Benar / Lanjut", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Ubah Nomor", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131D31),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF131D31),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Masukkan nomor telepon Anda",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chatin akan mengirimkan SMS resmi untuk memverifikasi nomor telepon Anda.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pemilih Negara (WhatsApp Style)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1A2640),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🇮🇩", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Indonesia",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "+62",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Nomor Telepon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A2640),
                    modifier = Modifier
                        .height(56.dp)
                        .width(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+62",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = rawPhoneInput,
                    onValueChange = {
                        val clean = it.replace("[^0-9]".toRegex(), "")
                        if (clean.length <= 13) {
                            rawPhoneInput = clean
                            inputError = null
                        }
                    },
                    placeholder = { Text("8xx xxxx xxxx", color = Color.White.copy(alpha = 0.35f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("whatsapp_phone_input_field")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Biaya SMS operator seluler mungkin berlaku",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f)
            )

            if (inputError != null || errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = inputError ?: errorMessage ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val clean = rawPhoneInput.trim().replace("[^0-9]".toRegex(), "")
                    if (clean.length < 8) {
                        inputError = "Masukkan nomor telepon yang valid (minimal 8 angka)"
                        return@Button
                    }
                    inputError = null
                    showConfirmDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("whatsapp_phone_submit_button")
            ) {
                Text(
                    text = "Lanjut",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF090D16)
                )
            }
        }
    }
}

/**
 * TAHAP 2: VERIFIKASI KODE OTP (PERSIS WHATSAPP)
 */
@Composable
fun WhatsAppOtpVerifyView(
    phoneNumber: String,
    errorMessage: String?,
    onVerify: (code: String) -> Unit,
    onResend: () -> Unit,
    onBackToPhoneInput: () -> Unit
) {
    var otpInput by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(60) }

    LaunchedEffect(Unit) {
        countdown = 60
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF131D31),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToPhoneInput) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Ubah Nomor",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Verifikasi Nomor Anda",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Menunggu SMS kode verifikasi 6-angka yang dikirim ke:",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = phoneNumber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salah nomor?",
                    fontSize = 13.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onBackToPhoneInput() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Kotak Input 6-Digit OTP WhatsApp Style
            OutlinedTextField(
                value = otpInput,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    if (clean.length <= 6) {
                        otpInput = clean
                        if (clean.length == 6) {
                            onVerify(clean)
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "• • •  • • •",
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedTextColor = Color(0xFF10B981),
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 10.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("whatsapp_otp_input_field")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = { onVerify(otpInput) },
                enabled = otpInput.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("whatsapp_submit_otp_button")
            ) {
                Text(
                    text = "Verifikasi Kode",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF090D16)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (countdown > 0) {
                Text(
                    text = "Kirim ulang SMS dalam $countdown detik",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                TextButton(
                    onClick = {
                        countdown = 60
                        onResend()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kirim Ulang Kode OTP",
                        fontSize = 13.sp,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * TAHAP 3: INFO PROFIL (PERSIS WHATSAPP)
 */
@Composable
fun WhatsAppProfileSetupView(
    phoneNumber: String,
    onComplete: (displayName: String, bio: String, avatarUrl: String) -> Unit
) {
    var displayNameInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf("Ada di Chatin • Komunikasi aman All-in-One") }
    var avatarUriString by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUriString = uri.toString()
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF131D31),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Info Profil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Harap berikan nama Anda dan foto profil opsional.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Avatar Bulat WhatsApp Style dengan Ikon Kamera
            Box(contentAlignment = Alignment.BottomEnd) {
                if (avatarUriString.isNotEmpty()) {
                    AsyncImage(
                        model = Uri.parse(avatarUriString),
                        contentDescription = "Foto Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF00E5FF), CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .border(2.dp, Color(0xFF131D31), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Pilih Foto",
                        tint = Color(0xFF090D16),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = displayNameInput,
                onValueChange = {
                    displayNameInput = it
                    inputError = null
                },
                label = { Text("Ketik nama Anda di sini") },
                placeholder = { Text("Nama Anda") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whatsapp_profile_name_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = bioInput,
                onValueChange = { bioInput = it },
                label = { Text("Info Status") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (inputError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = inputError!!,
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val cleanName = displayNameInput.trim()
                    if (cleanName.isEmpty()) {
                        inputError = "Silakan masukkan nama tampilan Anda"
                        return@Button
                    }
                    onComplete(cleanName, bioInput.trim(), avatarUriString)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("whatsapp_finish_profile_button")
            ) {
                Text(
                    text = "Lanjut",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF090D16)
                )
            }
        }
    }
}
