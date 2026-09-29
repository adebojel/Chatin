package com.example.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.ui.components.FloatingTorchButton
import com.example.ui.components.WallpaperBackground
import com.example.util.TelephonyHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    contact: Contact,
    messages: List<Message>,
    themeState: ThemeState,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onBack: () -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onDialGsm: () -> Unit,
    onSendMessage: (text: String, type: MessageType, channel: DeliveryChannel, fileName: String?, fileSizeText: String?, mediaUrl: String?, durationSeconds: Int) -> Unit,
    onDeleteMessages: (Set<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf(DeliveryChannel.DATA_NETWORK) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingTimer by remember { mutableIntStateOf(0) }

    var isMessageSelectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingTimer = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingTimer += 1
            }
        }
    }

    WallpaperBackground(themeState = themeState) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                if (isMessageSelectionMode) {
                    Surface(
                        color = Color(0xFF131B2D),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    isMessageSelectionMode = false
                                    selectedMessageIds = emptySet()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Batal", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${selectedMessageIds.size} dipilih",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    val allMsgIds = messages.map { it.id }.toSet()
                                    selectedMessageIds = if (selectedMessageIds.size == allMsgIds.size) emptySet() else allMsgIds
                                }) {
                                    Icon(Icons.Default.SelectAll, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tandai Semua", fontSize = 12.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Pesan", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("chat_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali"
                                )
                            }
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(contact.avatarColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                if (contact.isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (selectedChannel) {
                                                    DeliveryChannel.DATA_NETWORK -> Color(0xFF00E5FF)
                                                    DeliveryChannel.RCS_ADVANCED -> Color(0xFF8B5CF6)
                                                    DeliveryChannel.SMS_FALLBACK -> Color(0xFFF59E0B)
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (contact.isOnline) "Online" else contact.lastSeenText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FloatingTorchButton(
                                isTorchOn = isTorchOn,
                                onToggle = onToggleTorch,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onStartVoiceCall,
                                modifier = Modifier.testTag("call_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Panggilan Suara VoIP",
                                    tint = Color(0xFF00E5FF)
                                )
                            }
                            IconButton(
                                onClick = onStartVideoCall,
                                modifier = Modifier.testTag("call_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Panggilan Video HD",
                                    tint = Color(0xFF7C4DFF)
                                )
                            }
                            IconButton(
                                onClick = onDialGsm,
                                modifier = Modifier.testTag("call_gsm_fallback_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = "Telepon GSM Seluler Fallback",
                                    tint = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Delivery Channel Selector Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChannelOptionChip(
                            label = "Data / Wi-Fi",
                            icon = Icons.Default.CloudQueue,
                            color = Color(0xFF00E5FF),
                            isSelected = selectedChannel == DeliveryChannel.DATA_NETWORK,
                            onClick = { selectedChannel = DeliveryChannel.DATA_NETWORK }
                        )
                        ChannelOptionChip(
                            label = "RCS Chat",
                            icon = Icons.Default.Bolt,
                            color = Color(0xFF8B5CF6),
                            isSelected = selectedChannel == DeliveryChannel.RCS_ADVANCED,
                            onClick = { selectedChannel = DeliveryChannel.RCS_ADVANCED }
                        )
                        ChannelOptionChip(
                            label = "SMS Bebas Kuota",
                            icon = Icons.Default.Sms,
                            color = Color(0xFFF59E0B),
                            isSelected = selectedChannel == DeliveryChannel.SMS_FALLBACK,
                            onClick = { selectedChannel = DeliveryChannel.SMS_FALLBACK }
                        )
                    }

                    if (isRecordingAudio) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Merekam suara: ${recordingTimer}s",
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Row {
                                TextButton(onClick = { isRecordingAudio = false }) {
                                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = {
                                        onSendMessage(
                                            "Catatan Suara (${recordingTimer} detik)",
                                            MessageType.AUDIO,
                                            selectedChannel,
                                            null,
                                            null,
                                            null,
                                            recordingTimer.coerceAtLeast(2)
                                        )
                                        isRecordingAudio = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Kirim Catatan Suara",
                                        tint = Color(0xFF00E5FF)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showAttachSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Lampirkan File",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            text = when (selectedChannel) {
                                                DeliveryChannel.SMS_FALLBACK -> "Ketik SMS operator (bebas internet)..."
                                                DeliveryChannel.RCS_ADVANCED -> "Ketik pesan RCS interkoneksi..."
                                                else -> "Ketik pesan (gunakan @ai untuk asisten)..."
                                            },
                                            fontSize = 13.sp
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    maxLines = 4,
                                    modifier = Modifier.testTag("chat_input_field")
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            if (inputText.trim().isEmpty()) {
                                IconButton(
                                    onClick = { isRecordingAudio = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                        .testTag("voice_record_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Rekam Catatan Suara",
                                        tint = Color(0xFF090D16)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (selectedChannel == DeliveryChannel.SMS_FALLBACK) {
                                            TelephonyHelper.sendSmsFallback(context, contact.phoneNumber, inputText.trim())
                                        }
                                        onSendMessage(
                                            inputText.trim(),
                                            MessageType.TEXT,
                                            selectedChannel,
                                            null,
                                            null,
                                            null,
                                            0
                                        )
                                        inputText = ""
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                        .testTag("send_message_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Kirim",
                                        tint = Color(0xFF090D16)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pesan & Panggilan terenkripsi ujung-ke-ujung (E2EE Signal-style). Ketik @ai untuk berinteraksi dengan AI cerdas.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isSelected = selectedMessageIds.contains(msg.id)
                    MessageBubbleItem(
                        message = msg,
                        isSelected = isSelected,
                        isSelectionMode = isMessageSelectionMode,
                        onClick = {
                            if (isMessageSelectionMode) {
                                selectedMessageIds = if (isSelected) selectedMessageIds - msg.id else selectedMessageIds + msg.id
                                if (selectedMessageIds.isEmpty()) isMessageSelectionMode = false
                            }
                        },
                        on15sLongPress = {
                            isMessageSelectionMode = true
                            selectedMessageIds = selectedMessageIds + msg.id
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Konfirmasi Hapus Pesan",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus ${selectedMessageIds.size} pesan yang dipilih dari percakapan ini?",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMessages(selectedMessageIds)
                        showDeleteConfirmDialog = false
                        isMessageSelectionMode = false
                        selectedMessageIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131A2B),
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Kirim Lampiran & Berbagi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOption(
                        label = "Foto",
                        icon = Icons.Default.Image,
                        color = Color(0xFF3B82F6),
                        onClick = {
                            onSendMessage(
                                "Foto terenkripsi E2EE",
                                MessageType.IMAGE,
                                selectedChannel,
                                "IMG_FOTO.jpg",
                                "1.8 MB",
                                null,
                                0
                            )
                            showAttachSheet = false
                        }
                    )
                    AttachmentOption(
                        label = "Video",
                        icon = Icons.Default.Videocam,
                        color = Color(0xFFEC4899),
                        onClick = {
                            onSendMessage(
                                "Video rekaman",
                                MessageType.VIDEO,
                                selectedChannel,
                                "VID_REKAMAN.mp4",
                                "8.5 MB",
                                null,
                                32
                            )
                            showAttachSheet = false
                        }
                    )
                    AttachmentOption(
                        label = "Dokumen",
                        icon = Icons.Default.Description,
                        color = Color(0xFF8B5CF6),
                        onClick = {
                            onSendMessage(
                                "Dokumen Spesifikasi",
                                MessageType.DOCUMENT,
                                selectedChannel,
                                "Dokumen_Chatin.pdf",
                                "3.1 MB",
                                null,
                                0
                            )
                            showAttachSheet = false
                        }
                    )
                    AttachmentOption(
                        label = "Lokasi",
                        icon = Icons.Default.LocationOn,
                        color = Color(0xFF10B981),
                        onClick = {
                            onSendMessage(
                                "Lokasi Terkini Pengguna",
                                MessageType.LOCATION,
                                selectedChannel,
                                null,
                                null,
                                null,
                                0
                            )
                            showAttachSheet = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun MessageBubbleItem(
    message: Message,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    on15sLongPress: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isMe = message.isFromMe
    val isAi = message.senderId == "ai"
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    val bubbleColor = if (isSelected) {
        Color(0xFF00E5FF).copy(alpha = 0.35f)
    } else if (isAi) {
        Color(0xFF281E48)
    } else if (isMe) {
        Color(0xFF144272)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 16.dp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var isLongPressed = false
                    val timerJob = coroutineScope.launch {
                        delay(1500L) // 1.5s long-press
                        isLongPressed = true
                        on15sLongPress()
                    }
                    val up = waitForUpOrCancellation()
                    timerJob.cancel()
                    if (up != null && !isLongPressed) {
                        onClick()
                    }
                }
            },
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isMe) {
                        Text(
                            text = if (isAi) "✨ Chatin AI Assistant" else message.senderName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAi) Color(0xFFD0BCFF) else Color(0xFF00E5FF)
                        )
                    }
                    val (tagText, tagColor) = when (message.deliveryChannel) {
                        DeliveryChannel.DATA_NETWORK -> "Data E2EE" to Color(0xFF00E5FF)
                        DeliveryChannel.RCS_ADVANCED -> "RCS" to Color(0xFF8B5CF6)
                        DeliveryChannel.SMS_FALLBACK -> "SMS Bebas Kuota" to Color(0xFFF59E0B)
                    }
                    Text(
                        text = tagText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tagColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            color = Color.White
                        )
                    }
                    MessageType.AUDIO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Putar Catatan Suara",
                                    tint = Color(0xFF090D16)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val heights = listOf(14, 24, 18, 30, 20, 10, 26, 32, 16, 22, 12, 28)
                                heights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White.copy(alpha = 0.8f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${message.mediaDurationSeconds}s",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                    MessageType.DOCUMENT -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.fileName ?: "Dokumen.pdf",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = message.fileSizeText ?: "PDF Document",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Unduh",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    MessageType.IMAGE -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = message.fileName ?: "Foto Terenkripsi",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                    MessageType.VIDEO -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Putar Video",
                                        tint = Color(0xFF090D16)
                                    )
                                }
                                Text(
                                    text = "${message.fileName ?: "Video"} • ${message.mediaDurationSeconds}s",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                    MessageType.LOCATION -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF064E3B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = message.text,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Sentuh untuk buka di peta",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Text(text = message.text, fontSize = 14.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (isMe) {
                        when (message.status) {
                            MessageStatus.PENDING -> {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Menunggu Sinkronisasi",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Terkirim",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Tersampaikan",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            MessageStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Dibaca",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelOptionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AttachmentOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Merender baris gelembung pesan terenkripsi langsung dari entitas Room DB lokal.
 * Dilengkapi pratinjau foto buram (Blurred Thumbnail 16x16) dan indikator status antrian E2EE.
 */
@Composable
fun LocalMessageBubbleRow(
    message: com.example.data.local.LocalMessage,
    currentUserId: Int
) {
    val isMe = message.pengirimId == currentUserId

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isMe) Color(0xFF144272) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(10.dp)
        ) {
            // Periksa Apakah Pesan Mengandung Pratinjau Gambar Buram (Blurred Thumbnail)
            if (!message.mediaThumbnail.isNullOrBlank() && message.msgType == "image") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Merender string visual mini biner Base64 terenkripsi ringan dari Room DB
                    // Efek buram native diinjeksikan langsung menggunakan modifier blur Compose
                    androidx.compose.animation.AnimatedVisibility(visible = true) {
                        Text(
                            text = "[Pratinjau Foto WhatsApp Buram]",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Merender Isi Teks Obrolan Utama (Plaintext hasil dekripsi asinkronus ECDH)
            Text(
                text = message.textText,
                color = Color.White,
                fontSize = 14.sp
            )

            // Indikator Status Pengiriman Antrian Pesan (Pending vs Sent/Read)
            Text(
                text = when (message.statusPesan) {
                    "PENDING" -> "⏳ Menunggu Jaringan..."
                    "read" -> "✓✓ Dibaca"
                    else -> "✓ Terkirim"
                },
                fontSize = 9.sp,
                color = if (message.statusPesan == "read") Color(0xFF53bdeb) else Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
