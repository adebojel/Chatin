package com.example.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.models.AuthStep
import com.example.data.models.CallMediaType
import com.example.ui.components.*
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.call.VideoCallScreen
import com.example.ui.screens.call.VoiceCallScreen
import com.example.ui.screens.calls_history.CallsHistoryScreen
import com.example.ui.screens.chat.ChatDetailScreen
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.dialer.DialerScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.viewmodels.ChatinViewModel

enum class MainTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    CHATS("Obrolan", Icons.Filled.Chat, Icons.Outlined.Chat),
    CALLS("Panggilan", Icons.Filled.Call, Icons.Outlined.Call),
    DIALER("Telepon GSM", Icons.Filled.Dialpad, Icons.Outlined.Dialpad),
    PROFILE("Profil", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

@Composable
fun MainAppScaffold(
    viewModel: ChatinViewModel,
    onTriggerBiometricAuth: ((onSuccess: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(MainTab.CHATS) }

    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val visibleConversations by viewModel.visibleConversations.collectAsStateWithLifecycle()
    val hiddenConversations by viewModel.hiddenConversations.collectAsStateWithLifecycle()
    val securityState by viewModel.securityState.collectAsStateWithLifecycle()
    val linkedDevices by viewModel.linkedDevices.collectAsStateWithLifecycle()
    val isHiddenChatsUnlocked by viewModel.isHiddenChatsUnlocked.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val callHistory by viewModel.callHistory.collectAsStateWithLifecycle()
    val themeState by viewModel.themeState.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val networkState by viewModel.networkState.collectAsStateWithLifecycle()
    val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
    val currentMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()
    val activeCallState by viewModel.activeCallState.collectAsStateWithLifecycle()

    var showLinkedDevicesDialog by remember { mutableStateOf(false) }
    var showHiddenChatDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDefaultAppDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showContactQrDialog by remember { mutableStateOf(false) }
    var showNewChatSheet by remember { mutableStateOf(false) }
    var showProfileQuickSheet by remember { mutableStateOf(false) }
    var showChangePhoneDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showGoogleBackupDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var googleEmailInput by remember { mutableStateOf("pengguna@gmail.com") }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateProfile(
                authUiState.user?.displayName ?: "",
                authUiState.user?.bio ?: "Ada di Chatin • Komunikasi aman All-in-One",
                uri.toString()
            )
            Toast.makeText(context, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        }
    }

    // Gate Autentikasi: Tampilkan AuthScreen jika belum login
    if (authUiState.currentStep != AuthStep.AUTHENTICATED || authUiState.user == null) {
        val currentActivity = context as? android.app.Activity
        AuthScreen(
            authUiState = authUiState,
            onRequestPhoneLogin = { phone -> viewModel.requestPhoneLogin(phone, currentActivity) },
            onVerifyOtp = { c -> viewModel.verifyOtp(c) },
            onResendOtp = { viewModel.resendOtp(currentActivity) },
            onCompleteProfile = { d, b, a -> viewModel.completeProfile(d, b, a) },
            onSwitchStep = { s -> viewModel.setAuthStep(s) }
        )
        return
    }

    val totalUnread = remember(visibleConversations) {
        visibleConversations.sumOf { it.unreadCount }
    }

    // Overlay Layar Panggilan Aktif
    if (activeCallState != null && activeCallState?.isActive == true) {
        val call = activeCallState!!
        if (call.mediaType == CallMediaType.VIDEO) {
            VideoCallScreen(
                callState = call,
                isTorchOn = isTorchOn,
                onToggleTorch = { viewModel.toggleFlashlight() },
                onUpdateOptions = { viewModel.updateCallOptions(it) },
                onSetGreenScreenUri = { viewModel.setGreenScreenWallpaper(it) },
                onEndCall = { viewModel.endCall() },
                onSwitchToGsm = { viewModel.switchToGsmDialer() }
            )
        } else {
            VoiceCallScreen(
                callState = call,
                isTorchOn = isTorchOn,
                onToggleTorch = { viewModel.toggleFlashlight() },
                onUpdateOptions = { viewModel.updateCallOptions(it) },
                onEndCall = { viewModel.endCall() },
                onSwitchToGsm = { viewModel.switchToGsmDialer() }
            )
        }
        return
    }

    // Detail Percakapan Terpilih
    if (selectedChatId != null) {
        val currentConv = conversations.find { it.id == selectedChatId }
        val contact = currentConv?.contact ?: contacts.firstOrNull() ?: com.example.data.models.Contact(
            id = "c_direct",
            name = "Kontak Tujuan",
            phoneNumber = "+62 812",
            chatinId = "@user"
        )
        ChatDetailScreen(
            contact = contact,
            messages = currentMessages,
            themeState = themeState,
            isTorchOn = isTorchOn,
            onToggleTorch = { viewModel.toggleFlashlight() },
            onBack = { viewModel.closeChat() },
            onStartVoiceCall = {
                viewModel.startCall(contact.name, contact.phoneNumber, CallMediaType.AUDIO)
            },
            onStartVideoCall = {
                viewModel.startCall(contact.name, contact.phoneNumber, CallMediaType.VIDEO)
            },
            onDialGsm = {
                viewModel.dialContactGsm(contact.phoneNumber)
            },
            onSendMessage = { text, type, channel, fileName, fileSizeText, mediaUrl, duration ->
                viewModel.sendMessage(
                    text = text,
                    type = type,
                    channel = channel,
                    fileName = fileName,
                    fileSizeText = fileSizeText,
                    mediaUrl = mediaUrl,
                    durationSeconds = duration
                )
            },
            onDeleteMessages = { msgIds ->
                viewModel.deleteMessages(currentConv?.id ?: selectedChatId!!, msgIds)
            }
        )
        return
    }

    // Scaffold Beranda Utama
    WallpaperBackground(themeState = themeState) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Ketuk 2x Logo untuk membuka Ruang Obrolan Rahasia
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTimestamp < 450) {
                                        showHiddenChatDialog = true
                                    }
                                    lastTapTimestamp = now
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("app_logo_title")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = null,
                                    tint = Color(0xFF090D16),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "chatin",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                            if (hiddenConversations.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF2A85))
                                )
                            }
                        }

                        // Right Controls: Network Badge, White Chat Button, QR Scanner, Profile Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Status Jaringan Nyata Android OS
                            NetworkStatusBadge(
                                activeNetwork = networkState,
                                onSelectNetwork = { /* real status */ }
                            )

                            // Tombol Chat Putih Melingkar
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                                    .clickable { showNewChatSheet = true }
                                    .testTag("top_white_chat_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Mencari Kontak atau Buat Chat Baru",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // Ikon QR: Scan kontak & tampilkan QR saya
                            IconButton(
                                onClick = { showContactQrDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape)
                                    .testTag("top_qr_scanner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Pindai QR & Tukar Kontak",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Tombol Profil Cepat
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00E5FF),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF00E5FF), CircleShape)
                                    .clickable { showProfileQuickSheet = true }
                                    .testTag("top_profile_button")
                            ) {
                                if (!authUiState.user?.avatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = authUiState.user?.avatarUrl,
                                        contentDescription = "Tombol Profil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = authUiState.user?.displayName?.take(1)?.uppercase() ?: "P",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color(0xFF090D16)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { currentTab = tab }
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                                    .testTag("tab_${tab.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (tab == MainTab.CHATS && totalUnread > 0) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 6.dp, y = (-4).dp)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E5FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = totalUnread.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF090D16)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    MainTab.CHATS -> {
                        ChatListScreen(
                            conversations = visibleConversations,
                            contacts = contacts,
                            messagesMap = viewModel.messagesMap.collectAsStateWithLifecycle().value,
                            onSelectChat = { chatId -> viewModel.selectChat(chatId) },
                            onStartNewChatWithIdentifier = { identifier ->
                                viewModel.startOrGetChat(identifier)
                            },
                            onAddNewContact = { name, phone, username ->
                                viewModel.addContact(name, phone, username)
                            },
                            onHideChat = { chatId -> viewModel.toggleHideChat(chatId) },
                            onArchiveConversations = { chatIds, archive -> viewModel.archiveConversations(chatIds, archive) },
                            onDeleteConversations = { chatIds -> viewModel.deleteConversations(chatIds) },
                            onPinConversations = { chatIds -> viewModel.pinConversations(chatIds) },
                            onSyncContacts = { viewModel.syncDeviceContacts(context) },
                            onOpenDialer = { currentTab = MainTab.DIALER }
                        )
                    }
                    MainTab.CALLS -> {
                        CallsHistoryScreen(
                            callHistory = callHistory,
                            onStartCall = { name, number, type, forceGsm ->
                                viewModel.startCall(name, number, type, forceGsm)
                            }
                        )
                    }
                    MainTab.DIALER -> {
                        DialerScreen(
                            onDialGsm = { number -> viewModel.dialContactGsm(number) },
                            onDialVoip = { number, type ->
                                viewModel.startCall("Nomor $number", number, type)
                            },
                            onSendSms = { number -> viewModel.sendSmsFallback(number) }
                        )
                    }
                    MainTab.PROFILE -> {
                        ProfileScreen(
                            user = authUiState.user,
                            themeState = themeState,
                            onUpdateProfile = { name, bio, avatar -> viewModel.updateProfile(name, bio, avatar) },
                            onRequestChangePhoneNumber = { newPhone, callback ->
                                viewModel.requestChangePhoneNumber(newPhone, callback)
                            },
                            onVerifyChangePhoneNumber = { newPhone, code, callback ->
                                viewModel.verifyAndCommitNewPhoneNumber(newPhone, code, callback)
                            },
                            onBackupChatToGoogle = { email -> viewModel.backupChatToGoogle(email) },
                            onUpdatePrivacy = { lastSeen, readReceipts -> viewModel.updatePrivacySettings(lastSeen, readReceipts) },
                            onUpdateStorage = { wifi, cell -> viewModel.updateStorageSettings(wifi, cell) },
                            onUpdateAccessibility = { scale, contrast, displayScale -> viewModel.updateAccessibilitySettings(scale, contrast, displayScale) },
                            onOpenQrDialog = { showContactQrDialog = true },
                            onOpenLinkedDevicesDialog = { showLinkedDevicesDialog = true },
                            onOpenSecretChatDialog = { showHiddenChatDialog = true },
                            onOpenThemeDialog = { showThemeDialog = true },
                            onOpenDefaultAppDialog = { showDefaultAppDialog = true },
                            onLogout = { viewModel.logout() }
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showNewChatSheet) {
            NewChatContactPickerSheet(
                contacts = contacts,
                onSelectContact = { identifier ->
                    viewModel.startOrGetChat(identifier)
                    showNewChatSheet = false
                },
                onAddNewContact = { name, phone, username ->
                    viewModel.addContact(name, phone, username)
                },
                onSyncDeviceContacts = {
                    viewModel.syncDeviceContacts(context)
                },
                onDismiss = { showNewChatSheet = false }
            )
        }

        if (showProfileQuickSheet) {
            ProfileQuickMenuSheet(
                user = authUiState.user,
                onOpenFullProfile = {
                    currentTab = MainTab.PROFILE
                },
                onEditName = {
                    nameInput = authUiState.user?.displayName ?: ""
                    showEditNameDialog = true
                },
                onChangePhoto = {
                    photoPickerLauncher.launch("image/*")
                },
                onChangePhone = {
                    showChangePhoneDialog = true
                },
                onGoogleBackup = {
                    googleEmailInput = authUiState.user?.googleAccountEmail.takeIf { !it.isNullOrBlank() } ?: "pengguna@gmail.com"
                    showGoogleBackupDialog = true
                },
                onPrivacy = {
                    showPrivacyDialog = true
                },
                onStorage = {
                    showStorageDialog = true
                },
                onAccessibility = {
                    showAccessibilityDialog = true
                },
                onLinkedDevices = {
                    showLinkedDevicesDialog = true
                },
                onQrCode = {
                    showContactQrDialog = true
                },
                onDefaultApp = {
                    showDefaultAppDialog = true
                },
                onTheme = {
                    showThemeDialog = true
                },
                onSecretChat = {
                    showHiddenChatDialog = true
                },
                onLogout = {
                    viewModel.logout()
                },
                onDismiss = { showProfileQuickSheet = false }
            )
        }

        if (showChangePhoneDialog) {
            ChangePhoneNumberDialog(
                user = authUiState.user,
                onRequestOtp = { newPhone, callback ->
                    viewModel.requestChangePhoneNumber(newPhone, callback)
                },
                onVerifyOtp = { newPhone, code, callback ->
                    viewModel.verifyAndCommitNewPhoneNumber(newPhone, code, callback)
                },
                onDismiss = { showChangePhoneDialog = false }
            )
        }

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
                                viewModel.updateProfile(nameInput.trim(), authUiState.user?.bio ?: "", authUiState.user?.avatarUrl ?: "")
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
                            text = "Cadangkan seluruh riwayat pesan obrolan, kontak, dan media ke Google Drive atau ekspor ke Akun Google Anda.",
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.backupChatToGoogle(googleEmailInput.trim())
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

        if (showPrivacyDialog) {
            var selectedLastSeen by remember { mutableStateOf(authUiState.user?.privacyLastSeen ?: "Semua Orang") }
            var readReceipts by remember { mutableStateOf(authUiState.user?.privacyReadReceipts ?: true) }
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Pengaturan Privasi", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column {
                        Text("Siapa yang dapat melihat 'Terakhir Dilihat':", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
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
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Laporan Dibaca (Centang Biru)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("Kirimkan tanda centang saat dibaca", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
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
                            viewModel.updatePrivacySettings(selectedLastSeen, readReceipts)
                            showPrivacyDialog = false
                            Toast.makeText(context, "Pengaturan privasi diperbarui", Toast.LENGTH_SHORT).show()
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

        if (showStorageDialog) {
            var wifiAuto by remember { mutableStateOf(authUiState.user?.mediaAutoDownloadWifi ?: true) }
            var cellAuto by remember { mutableStateOf(authUiState.user?.mediaAutoDownloadCellular ?: false) }
            AlertDialog(
                onDismissRequest = { showStorageDialog = false },
                title = { Text("Penyimpanan dan Data", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pengaturan Unduh Otomatis Media:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Unduh Otomatis saat Wi-Fi", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = wifiAuto,
                                onCheckedChange = { wifiAuto = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Unduh Otomatis saat Data Seluler", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = cellAuto,
                                onCheckedChange = { cellAuto = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateStorageSettings(wifiAuto, cellAuto)
                            showStorageDialog = false
                            Toast.makeText(context, "Pengaturan penyimpanan disimpan", Toast.LENGTH_SHORT).show()
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

        if (showAccessibilityDialog) {
            var fontScale by remember { mutableFloatStateOf(authUiState.user?.fontSizeScale ?: 1.0f) }
            var highContrast by remember { mutableStateOf(authUiState.user?.highContrastMode ?: false) }
            AlertDialog(
                onDismissRequest = { showAccessibilityDialog = false },
                title = { Text("Pengaturan Aksesibilitas", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Skala Ukuran Teks: ${(fontScale * 100).toInt()}%", fontSize = 13.sp, color = Color.White)
                        Slider(
                            value = fontScale,
                            onValueChange = { fontScale = it },
                            valueRange = 0.85f..1.4f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mode Kontras Tinggi", color = Color.White, fontSize = 13.sp)
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
                            viewModel.updateAccessibilitySettings(fontScale, highContrast)
                            showAccessibilityDialog = false
                            Toast.makeText(context, "Pengaturan aksesibilitas diterapkan", Toast.LENGTH_SHORT).show()
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

        if (showContactQrDialog) {
            ContactQrDialog(
                user = authUiState.user,
                onContactScanned = { identifier ->
                    viewModel.startOrGetChat(identifier)
                },
                onDismiss = { showContactQrDialog = false }
            )
        }

        if (showDefaultAppDialog) {
            DefaultAppDialog(
                onDismiss = { showDefaultAppDialog = false }
            )
        }

        if (showCloudSyncDialog) {
            CloudSyncDialog(
                user = authUiState.user,
                onSyncNow = { viewModel.syncCloudServerData() },
                onUpdateProfile = { name, bio, avatar -> viewModel.updateProfile(name, bio, avatar) },
                onDismiss = { showCloudSyncDialog = false }
            )
        }

        if (showLinkedDevicesDialog) {
            LinkedDevicesDialog(
                linkedDevices = linkedDevices,
                onLinkNewDevice = { browser, os, loc -> viewModel.linkNewWebDevice(browser, os, loc) },
                onUnlinkDevice = { viewModel.unlinkDevice(it) },
                onDismiss = { showLinkedDevicesDialog = false }
            )
        }

        if (showHiddenChatDialog) {
            HiddenChatSecurityDialog(
                securityState = securityState,
                hiddenConversations = hiddenConversations,
                allConversations = conversations,
                isUnlocked = isHiddenChatsUnlocked,
                onVerifyCode = { viewModel.verifyUnlockCode(it) },
                onTriggerBiometricAuth = onTriggerBiometricAuth,
                onBiometricUnlockSuccess = { viewModel.unlockHiddenChatsBiometric() },
                onUpdateSecurityConfig = { lockType, code -> viewModel.updateSecurityConfig(lockType, code) },
                onToggleHideChat = { viewModel.toggleHideChat(it) },
                onOpenChat = { chatId -> viewModel.selectChat(chatId) },
                onLockAgain = { viewModel.lockHiddenChats() },
                onDismiss = { showHiddenChatDialog = false }
            )
        }

        if (showThemeDialog) {
            ThemeQuickSettingsDialog(
                themeState = themeState,
                onSetThemeMode = { viewModel.setThemeMode(it) },
                onSetBackgroundPreset = { viewModel.setBackgroundPreset(it) },
                onSetGalleryWallpaper = { viewModel.setGalleryWallpaper(it) },
                onGenerateAiTheme = { viewModel.generateAiTheme(it) },
                onDismiss = { showThemeDialog = false }
            )
        }
    }
}
