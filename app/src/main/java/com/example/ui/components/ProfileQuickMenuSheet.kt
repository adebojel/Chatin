package com.example.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.models.UserAccount

@Composable
fun ProfileQuickMenuSheet(
    user: UserAccount?,
    onOpenFullProfile: () -> Unit,
    onEditName: () -> Unit,
    onChangePhoto: () -> Unit,
    onChangePhone: () -> Unit = {},
    onGoogleBackup: () -> Unit,
    onPrivacy: () -> Unit,
    onStorage: () -> Unit,
    onAccessibility: () -> Unit,
    onLinkedDevices: () -> Unit,
    onQrCode: () -> Unit,
    onDefaultApp: () -> Unit,
    onTheme: () -> Unit,
    onSecretChat: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
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
                // Top Header
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
                                .background(Color(0xFF00E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF090D16), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Profil & Pengaturan",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Akun resmi Chatin All-in-One",
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

                // User Identity Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF131F36),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                if (!user?.avatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = user?.avatarUrl,
                                        contentDescription = "Foto Profil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, Color(0xFF00E5FF), CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E5FF))
                                            .border(2.dp, Color(0xFF0D1424), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user?.displayName?.take(1)?.uppercase() ?: "U",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            color = Color(0xFF090D16)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onChangePhoto,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                        .testTag("quick_change_avatar_btn")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Ganti Foto", tint = Color(0xFF090D16), modifier = Modifier.size(12.dp))
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
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = onEditName,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Ganti Nama", tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                                    }
                                }
                                Text(
                                    text = "${user?.username ?: "@user"} • ${user?.phoneNumber ?: ""}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF00E5FF)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = user?.bio ?: "Ada di Chatin • Komunikasi aman All-in-One",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // QR Code Scanner & Exchange Button
                        Button(
                            onClick = {
                                onQrCode()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_quick_qr_btn")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF090D16), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pindai QR / Tukar Kontak Saya", color = Color(0xFF090D16), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Menu Items List
                Text(
                    text = "MENU & FITUR LENGKAP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF131F36),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // 1. Pengaturan Profil Lengkap
                        ProfileItemRow(
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFF00E5FF),
                            title = "Pengaturan Akun & Profil Lengkap",
                            subtitle = "Lihat halaman profil, status sinkron, dan info detail",
                            onClick = {
                                onOpenFullProfile()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 2. Ubah Nomor Telepon (2-Tahap OTP)
                        ProfileItemRow(
                            icon = Icons.Default.PhoneIphone,
                            iconColor = Color(0xFF10B981),
                            title = "Ubah Nomor Telepon (Verifikasi OTP)",
                            subtitle = "Perbarui nomor telepon dengan verifikasi SMS OTP",
                            onClick = {
                                onChangePhone()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 3. Ganti Nama
                        ProfileItemRow(
                            icon = Icons.Default.Badge,
                            iconColor = Color(0xFF38BDF8),
                            title = "Ganti Nama",
                            subtitle = user?.displayName?.ifBlank { user.phoneNumber } ?: "",
                            onClick = {
                                onEditName()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 4. Ganti Foto Profil
                        ProfileItemRow(
                            icon = Icons.Default.PhotoCamera,
                            iconColor = Color(0xFF06B6D4),
                            title = "Ganti Foto Profil",
                            subtitle = "Pilih foto dari galeri HP perangkat",
                            onClick = {
                                onChangePhoto()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 5. Bagikan Chat ke Akun Google
                        ProfileItemRow(
                            icon = Icons.Default.CloudUpload,
                            iconColor = Color(0xFF10B981),
                            title = "Bagikan Chat ke Akun Google",
                            subtitle = "Cadangkan ke Google Drive atau ekspor akun Google",
                            onClick = {
                                onGoogleBackup()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 6. Privasi
                        ProfileItemRow(
                            icon = Icons.Default.Security,
                            iconColor = Color(0xFFF59E0B),
                            title = "Privasi",
                            subtitle = "Terakhir dilihat, status online, centang laporan",
                            onClick = {
                                onPrivacy()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 7. Penyimpanan dan Data
                        ProfileItemRow(
                            icon = Icons.Default.DataUsage,
                            iconColor = Color(0xFFEC4899),
                            title = "Penyimpanan dan Data",
                            subtitle = "Unduh otomatis media Wi-Fi & Seluler",
                            onClick = {
                                onStorage()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 8. Aksesibilitas
                        ProfileItemRow(
                            icon = Icons.Default.AccessibilityNew,
                            iconColor = Color(0xFF8B5CF6),
                            title = "Aksesibilitas",
                            subtitle = "Ukuran font teks dan kontras tinggi",
                            onClick = {
                                onAccessibility()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 9. Perangkat Tertaut (Web / Desktop)
                        ProfileItemRow(
                            icon = Icons.Default.LaptopMac,
                            iconColor = Color(0xFF00E5FF),
                            title = "Perangkat Tertaut (Web / Desktop)",
                            subtitle = "Tautkan browser di komputer via kode QR",
                            onClick = {
                                onLinkedDevices()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 10. Jadikan Aplikasi Default
                        ProfileItemRow(
                            icon = Icons.Default.Verified,
                            iconColor = Color(0xFF10B981),
                            title = "Jadikan Aplikasi Default (SMS & Telepon)",
                            subtitle = "Aktifkan Chatin sebagai penangan utama",
                            onClick = {
                                onDefaultApp()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 11. Tema & Wallpaper
                        ProfileItemRow(
                            icon = Icons.Default.Palette,
                            iconColor = Color(0xFFF43F5E),
                            title = "Tema & Wallpaper Obrolan",
                            subtitle = "Dark Neon, Clean White, dan Wallpaper Galeri",
                            onClick = {
                                onTheme()
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                        // 12. Ruang Obrolan Rahasia
                        ProfileItemRow(
                            icon = Icons.Default.Lock,
                            iconColor = Color(0xFFFF2A85),
                            title = "Ruang Obrolan Rahasia (PIN/Pola/Sandi)",
                            subtitle = "Proteksi sandi khusus obrolan privat",
                            onClick = {
                                onSecretChat()
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Logout Button
                Button(
                    onClick = {
                        onLogout()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar dari Akun", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileItemRow(
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}
