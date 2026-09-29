package com.example.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AuthStep
import com.example.data.models.AuthUiState
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    authUiState: AuthUiState,
    onLogin: (username: String, pass: String) -> Unit,
    onRegister: (username: String, phone: String, pass: String) -> Unit,
    onVerifyOtp: (code: String) -> Unit,
    onResendOtp: () -> Unit,
    onCompleteProfile: (displayName: String, bio: String) -> Unit,
    onSwitchStep: (AuthStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF090D16)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D1527),
                            Color(0xFF090D16),
                            Color(0xFF060910)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                // App Brand Logo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Chatin Logo",
                        tint = Color(0xFF090D16),
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "chatin",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Platform Komunikasi Resmi All-in-One: WhatsApp, Messenger & SMS",
                    fontSize = 12.sp,
                    color = Color(0xFF00E5FF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = authUiState.currentStep,
                    label = "auth_step_transition"
                ) { step ->
                    when (step) {
                        AuthStep.LOGIN -> {
                            LoginView(
                                errorMessage = authUiState.errorMessage,
                                onLogin = onLogin,
                                onGoToRegister = { onSwitchStep(AuthStep.REGISTER) }
                            )
                        }
                        AuthStep.REGISTER -> {
                            RegisterView(
                                errorMessage = authUiState.errorMessage,
                                onRegister = onRegister,
                                onGoToLogin = { onSwitchStep(AuthStep.LOGIN) }
                            )
                        }
                        AuthStep.VERIFY_OTP -> {
                            OtpVerificationView(
                                phoneNumber = authUiState.pendingPhoneNumber,
                                latestOtp = authUiState.lastSentOtp,
                                errorMessage = authUiState.errorMessage,
                                onVerify = onVerifyOtp,
                                onResend = onResendOtp,
                                onBack = { onSwitchStep(AuthStep.REGISTER) }
                            )
                        }
                        AuthStep.PROFILE_SETUP -> {
                            ProfileSetupView(
                                defaultName = authUiState.pendingUsername,
                                onComplete = onCompleteProfile
                            )
                        }
                        AuthStep.AUTHENTICATED -> {
                            // Handled by main scaffold
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginView(
    errorMessage: String?,
    onLogin: (username: String, pass: String) -> Unit,
    onGoToRegister: () -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF131A2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Masuk ke Akun Anda",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Gunakan username atau nomor telepon Anda",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Username atau Nomor Telepon") },
                placeholder = { Text("cth: @aditya atau 08123456789") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF00E5FF))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_username_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Kata Sandi") },
                placeholder = { Text("Masukkan kata sandi Anda") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00E5FF))
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onLogin(usernameInput, passwordInput) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input")
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLogin(usernameInput, passwordInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("login_submit_button")
            ) {
                Icon(imageVector = Icons.Default.Login, contentDescription = null, tint = Color(0xFF090D16))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Masuk Sekarang",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF090D16)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Belum punya akun? ", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                Text(
                    text = "Daftar Baru",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier
                        .clickable { onGoToRegister() }
                        .padding(4.dp)
                        .testTag("go_to_register_link")
                )
            }
        }
    }
}

@Composable
fun RegisterView(
    errorMessage: String?,
    onRegister: (username: String, phone: String, pass: String) -> Unit,
    onGoToLogin: () -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("+62 ") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF131A2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daftar Akun Chatin Baru",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Verifikasi nomor ponsel untuk aktivasi fitur SMS & WhatsApp",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Username") },
                placeholder = { Text("cth: rizky_pratama") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = null, tint = Color(0xFF00E5FF))
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_username_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("Nomor Telepon (SMS & Panggilan)") },
                placeholder = { Text("+62 812-xxxx-xxxx") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF10B981))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_phone_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Kata Sandi") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00E5FF))
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPasswordInput,
                onValueChange = { confirmPasswordInput = it },
                label = { Text("Konfirmasi Kata Sandi") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF00E5FF))
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onRegister(usernameInput, phoneInput, passwordInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("register_submit_button")
            ) {
                Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = Color(0xFF090D16))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lanjut Verifikasi Nomor Telepon",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF090D16)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sudah punya akun? ", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                Text(
                    text = "Masuk Sekarang",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier
                        .clickable { onGoToLogin() }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun OtpVerificationView(
    phoneNumber: String,
    latestOtp: String,
    errorMessage: String?,
    onVerify: (code: String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    var otpInput by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(60) }

    LaunchedEffect(latestOtp) {
        countdown = 60
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF131A2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verifikasi SMS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkChatRead,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Masukkan Kode OTP 6-Digit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Kode verifikasi telah dikirim melalui SMS ke $phoneNumber",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Realistic SMS Notification Simulation Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E283D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("📩 SMS Masuk dari Chatin OTP", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            Text("Kode Anda: $latestOtp (Rahasiakan kode ini)", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    TextButton(onClick = {
                        otpInput = latestOtp
                        onVerify(latestOtp)
                    }) {
                        Text("Auto Fill", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = otpInput,
                onValueChange = {
                    if (it.length <= 6) {
                        otpInput = it
                        if (it.length == 6) {
                            onVerify(it)
                        }
                    }
                },
                placeholder = { Text("000000", fontSize = 24.sp, letterSpacing = 8.sp, textAlign = TextAlign.Center) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color(0xFF10B981),
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .testTag("otp_code_input")
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

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { onVerify(otpInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("verify_otp_button")
            ) {
                Text("Verifikasi & Masuk", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF090D16))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (countdown > 0) {
                    Text(
                        text = "Kirim ulang kode dalam $countdown detik",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    TextButton(onClick = onResend) {
                        Text("Kirim Ulang Kode OTP", fontSize = 12.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetupView(
    defaultName: String,
    onComplete: (displayName: String, bio: String) -> Unit
) {
    var displayNameInput by remember { mutableStateOf(defaultName) }
    var bioInput by remember { mutableStateOf("Ada di Chatin • Siap komunikasi ganda") }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF131A2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lengkapi Profil Akun Chatin",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Format nama akun lengkap seperti profil media sosial",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayNameInput.take(1).uppercase().ifBlank { "U" },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF090D16)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = displayNameInput,
                onValueChange = { displayNameInput = it },
                label = { Text("Nama Tampilan Akun") },
                placeholder = { Text("cth: Budi Santoso atau Andi Wijaya") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = bioInput,
                onValueChange = { bioInput = it },
                label = { Text("Status / Bio Akun") },
                placeholder = { Text("Status Anda...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onComplete(displayNameInput, bioInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("complete_profile_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF090D16))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Selesai & Buka Chatin",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF090D16)
                )
            }
        }
    }
}
