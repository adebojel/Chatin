package com.example.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.ThemeState
import com.example.data.models.UserAccount
import com.example.ui.components.ChangePhoneNumberDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    user: UserAccount?,
    themeState: ThemeState,
    onUpdateProfile: (displayName: String, bio: String, avatarUrl: String) -> Unit,
    onRequestChangePhoneNumber: (newPhone: String, onResult: (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onVerifyChangePhoneNumber: (newPhone: String, otpCode: String, onResult: (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onBackupChatToGoogle: (String) -> Unit,
    onUpdatePrivacy: (lastSeen: String, readReceipts: Boolean) -> Unit,
    onUpdateStorage: (autoDownloadWifi: Boolean, autoDownloadCellular: Boolean) -> Unit,
    onUpdateAccessibility: (fontScale: Float, highContrast: Boolean, displayScale: Float) -> Unit,
    onOpenQrDialog: () -> Unit,
    onOpenLinkedDevicesDialog: () -> Unit,
    onOpenSecretChatDialog: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onOpenDefaultAppDialog: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditBioDialog by remember { mutableStateOf(false) }
    var showChangePhoneDialog by remember { mutableStateOf(false) }
    var showGoogleBackupDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf(user?.displayName ?: "") }
    var bioInput by remember { mutableStateOf(user?.bio ?: "") }
    var googleEmailInput by remember { mutableStateOf(user?.googleAccountEmail.takeIf { !it.isNullOrBlank() } ?: "pengguna@gmail.com") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateProfile(
                user?.displayName ?: "",
                user?.bio ?: "Ada di Chatin • Komunikasi aman All-in-One",
                uri.toString()
            )
            Toast.makeText(context, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        }
    }

    val timeFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val lastBackupStr = remember(user?.lastGoogleBackupTime) {
        val time = user?.lastGoogleBackupTime ?: 0L
        if (time > 0L) timeFormat.format(Date(time)) else "Belum dicadangkan"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with QR Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Profil Saya",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Info akun, privasi, data, dan cadangan Google",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onOpenQrDialog,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("profile_qr_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "Kode QR Profil Saya & Pindai Kontak",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Profile Hero Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF131C2E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0A2540), Color(0xFF0284C7), Color(0xFF7C3AED))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-30).dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (!user?.avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = Uri.parse(user?.avatarUrl),
                                contentDescription = "Foto Profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, Color(0xFF131C2E), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF))
                                    .border(3.dp, Color(0xFF131C2E), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.displayName?.take(1)?.uppercase() ?: "U",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF090D16)
                                )
                            }
                        }
                        IconButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                                .border(1.5.dp, Color(0xFF131C2E), CircleShape)
                                .testTag("change_profile_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Ganti Foto Profil", tint = Color(0xFF090D16), modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = user?.displayName?.ifBlank { user.phoneNumber } ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            IconButton(
                                onClick = {
                                    nameInput = user?.displayName ?: ""
                                    showEditNameDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Ganti Nama", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = user?.phoneNumber ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-15).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user?.bio ?: "Ada di Chatin • Komunikasi aman All-in-One",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                bioInput = user?.bio ?: ""
                                showEditBioDialog = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Bio", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Section: Menu Items
        Text(
            text = "PENGATURAN & LAYANAN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF131C2E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // 1. Ubah Nomor Telepon (2-Tahap OTP Asli)
                ProfileSettingRow(
                    icon = Icons.Default.PhoneIphone,
                    iconColor = Color(0xFF10B981),
                    title = "Ubah Nomor Telepon (Verifikasi OTP)",
                    subtitle = "${user?.phoneNumber ?: "-"} • 2-Tahap Verifikasi SMS Asli",
                    onClick = { showChangePhoneDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 2. Ganti Nama
                ProfileSettingRow(
                    icon = Icons.Default.Badge,
                    iconColor = Color(0xFF00E5FF),
                    title = "Ganti Nama Akun",
                    subtitle = user?.displayName?.ifBlank { user.phoneNumber } ?: "",
                    onClick = {
                        nameInput = user?.displayName ?: ""
                        showEditNameDialog = true
                    }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 3. Ganti Foto Profil
                ProfileSettingRow(
                    icon = Icons.Default.PhotoCamera,
                    iconColor = Color(0xFF38BDF8),
                    title = "Ganti Foto Profil",
                    subtitle = "Pilih foto dari galeri perangkat ponsel",
                    onClick = { photoPickerLauncher.launch("image/*") }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 4. Bagikan Chat ke Akun Google
                ProfileSettingRow(
                    icon = Icons.Default.CloudUpload,
                    iconColor = Color(0xFF10B981),
                    title = "Bagikan Chat ke Akun Google",
                    subtitle = "Cadangan ke Google Drive: $lastBackupStr",
                    onClick = { showGoogleBackupDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 5. Privasi
                ProfileSettingRow(
                    icon = Icons.Default.Security,
                    iconColor = Color(0xFFF59E0B),
                    title = "Privasi",
                    subtitle = "Terakhir dilihat, info akun, dan laporan dibaca",
                    onClick = { showPrivacyDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 6. Penyimpanan dan Data
                ProfileSettingRow(
                    icon = Icons.Default.DataUsage,
                    iconColor = Color(0xFFEC4899),
                    title = "Penyimpanan dan Data",
                    subtitle = "Penggunaan jaringan, unduh otomatis media",
                    onClick = { showStorageDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 7. Ukuran Teks & Layar (Aksesibilitas)
                val fontPct = ((user?.fontSizeScale ?: 1.0f) * 100).toInt()
                val dispPct = ((user?.displayDensityScale ?: 1.0f) * 100).toInt()
                ProfileSettingRow(
                    icon = Icons.Default.AccessibilityNew,
                    iconColor = Color(0xFF8B5CF6),
                    title = "Ukuran Teks & Ukuran Layar",
                    subtitle = "Font teks: $fontPct% • Tampilan layar: $dispPct%",
                    onClick = { showAccessibilityDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 8. Perangkat Tertaut (Web / Desktop)
                ProfileSettingRow(
                    icon = Icons.Default.LaptopMac,
                    iconColor = Color(0xFF00E5FF),
                    title = "Perangkat Tertaut (Web / Desktop)",
                    subtitle = "Tautkan browser di komputer via kode QR",
                    onClick = onOpenLinkedDevicesDialog
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 9. Ruang Obrolan Rahasia
                ProfileSettingRow(
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFFFF2A85),
                    title = "Ruang Obrolan Rahasia (PIN/Pola/Sandi/Biometrik)",
                    subtitle = "Proteksi sandi khusus di balik ketuk 2x logo",
                    onClick = onOpenSecretChatDialog
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 10. Tema & Wallpaper
                ProfileSettingRow(
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFF00E5FF),
                    title = "Tema & Wallpaper Obrolan",
                    subtitle = "Dark Neon, Clean White, dan Latar Pemandangan",
                    onClick = onOpenThemeDialog
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                // 11. Jadikan Aplikasi Default
                ProfileSettingRow(
                    icon = Icons.Default.Verified,
                    iconColor = Color(0xFF10B981),
                    title = "Jadikan Aplikasi Default (SMS & Telepon)",
                    subtitle = "Aktifkan Chatin sebagai penangan panggilan & pesan utama",
                    onClick = onOpenDefaultAppDialog
                )
            }
        }

        // Logout Button
        Button(
            onClick = { showLogoutConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Keluar dari Akun", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Change Phone Number Dialog
    if (showChangePhoneDialog) {
        ChangePhoneNumberDialog(
            user = user,
            onRequestOtp = onRequestChangePhoneNumber,
            onVerifyOtp = onVerifyChangePhoneNumber,
            onDismiss = { showChangePhoneDialog = false }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Ganti Nama Akun", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Nama lengkap ini akan terlihat oleh semua kontak dan lawan bicara:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onUpdateProfile(nameInput.trim(), user?.bio ?: "", user?.avatarUrl ?: "")
                            showEditNameDialog = false
                            Toast.makeText(context, "Nama berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Simpan", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Edit Bio Dialog
    if (showEditBioDialog) {
        AlertDialog(
            onDismissRequest = { showEditBioDialog = false },
            title = { Text("Edit Status / Bio", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Tuliskan status atau bio profil Anda:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("Bio / Status") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(user?.displayName ?: "", bioInput.trim(), user?.avatarUrl ?: "")
                        showEditBioDialog = false
                        Toast.makeText(context, "Bio berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Simpan", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBioDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Google Backup Dialog
    if (showGoogleBackupDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Bagikan Chat ke Akun Google", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Cadangkan seluruh riwayat obrolan, kontak, dan media ke Google Drive atau ekspor ke Akun Google Anda.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Akun Email Google") },
                        placeholder = { Text("email@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E2A40),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Terakhir Dicadangkan:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(lastBackupStr, fontSize = 12.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBackupChatToGoogle(googleEmailInput.trim())
                        showGoogleBackupDialog = false
                        Toast.makeText(context, "Riwayat chat berhasil dicadangkan ke akun Google $googleEmailInput!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Cadangkan Sekarang", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleBackupDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        var selectedLastSeen by remember { mutableStateOf(user?.privacyLastSeen ?: "Semua Orang") }
        var readReceipts by remember { mutableStateOf(user?.privacyReadReceipts ?: true) }
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Pengaturan Privasi", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Siapa yang dapat melihat 'Terakhir Dilihat' (Last Seen):", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("Semua Orang", "Kontak Saya", "Tidak Ada").forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLastSeen = opt }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedLastSeen == opt,
                                onClick = { selectedLastSeen = opt },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(opt, color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Laporan Dibaca (Centang Biru)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Kirimkan tanda centang biru saat pesan dibaca", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = readReceipts,
                            onCheckedChange = { readReceipts = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePrivacy(selectedLastSeen, readReceipts)
                        showPrivacyDialog = false
                        Toast.makeText(context, "Setelan privasi disimpan", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Simpan", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Storage Dialog
    if (showStorageDialog) {
        var autoWifi by remember { mutableStateOf(user?.mediaAutoDownloadWifi ?: true) }
        var autoCellular by remember { mutableStateOf(user?.mediaAutoDownloadCellular ?: false) }
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Penyimpanan dan Data", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Unduh Otomatis Media:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saat Tersambung ke Wi-Fi", fontSize = 13.sp, color = Color.White)
                        Switch(checked = autoWifi, onCheckedChange = { autoWifi = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saat Menggunakan Data Seluler", fontSize = 13.sp, color = Color.White)
                        Switch(checked = autoCellular, onCheckedChange = { autoCellular = it })
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Penggunaan Jaringan Chatin:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• VoIP WebRTC: 24.8 MB terkirim\n• Pesan & SMS: 3.2 MB terkirim", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStorage(autoWifi, autoCellular)
                        showStorageDialog = false
                        Toast.makeText(context, "Setelan penyimpanan disimpan", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Simpan", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Accessibility Dialog
    if (showAccessibilityDialog) {
        var fontScale by remember { mutableFloatStateOf(user?.fontSizeScale ?: 1.0f) }
        var displayScale by remember { mutableFloatStateOf(user?.displayDensityScale ?: 1.0f) }
        var highContrast by remember { mutableStateOf(user?.highContrastMode ?: false) }
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ukuran Teks & Ukuran Layar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ukuran Teks (Font Size):", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                            Text("${(fontScale * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 13.sp)
                        }
                        Slider(
                            value = fontScale,
                            onValueChange = { fontScale = it },
                            valueRange = 0.8f..1.4f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ukuran Layar (Display Scale):", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                            Text("${(displayScale * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                        }
                        Slider(
                            value = displayScale,
                            onValueChange = { displayScale = it },
                            valueRange = 0.85f..1.25f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kontras Tinggi", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Tingkatkan ketajaman warna untuk kemudahan baca", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = highContrast,
                            onCheckedChange = { highContrast = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateAccessibility(fontScale, highContrast, displayScale)
                        showAccessibilityDialog = false
                        Toast.makeText(context, "Ukuran teks & layar berhasil disimpan", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Simpan", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Konfirmasi Keluar Akun", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin keluar dari akun Chatin ini?",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Keluar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ProfileSettingRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
    }
}
