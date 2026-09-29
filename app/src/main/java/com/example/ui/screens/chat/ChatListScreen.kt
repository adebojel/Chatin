package com.example.ui.screens.chat

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ChatConversation
import com.example.data.models.Contact
import com.example.data.models.DeliveryChannel
import com.example.data.models.Message
import com.example.ui.components.NewChatContactPickerSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    conversations: List<ChatConversation>,
    contacts: List<Contact>,
    messagesMap: Map<String, List<Message>> = emptyMap(),
    onSelectChat: (String) -> Unit,
    onStartNewChatWithIdentifier: (String) -> Unit,
    onAddNewContact: (name: String, phone: String, username: String) -> Unit = { _, _, _ -> },
    onHideChat: (String) -> Unit = {},
    onArchiveConversations: (Set<String>, Boolean) -> Unit = { _, _ -> },
    onDeleteConversations: (Set<String>) -> Unit = {},
    onPinConversations: (Set<String>) -> Unit = {},
    onSyncContacts: () -> Unit = {},
    onOpenDialer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    val filterTabs = listOf("Semua", "Chatin Data", "SMS Operator", "Belum Dibaca", "Diarsipkan")

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedChatIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showArchiveConfirmDialog by remember { mutableStateOf(false) }
    var showContactPickerSheet by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSyncContacts()
            Toast.makeText(context, "Kontak perangkat berhasil disinkronkan!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin kontak diperlukan untuk sinkronisasi buku telepon", Toast.LENGTH_SHORT).show()
        }
    }

    val matchingMessagesSnippetMap = remember(conversations, messagesMap, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) emptyMap<String, String>()
        else {
            val map = mutableMapOf<String, String>()
            conversations.forEach { conv ->
                val matchingMsg = messagesMap[conv.id]?.firstOrNull { msg ->
                    msg.text.contains(query, ignoreCase = true) ||
                            (msg.fileName?.contains(query, ignoreCase = true) == true)
                }
                if (matchingMsg != null) {
                    val snippet = if (matchingMsg.text.isNotBlank()) matchingMsg.text else matchingMsg.fileName ?: ""
                    map[conv.id] = snippet
                }
            }
            map
        }
    }

    val filteredConversations = remember(conversations, messagesMap, searchQuery, selectedFilter) {
        val query = searchQuery.trim()
        conversations.filter { conv ->
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                val contactNameMatch = conv.contact.name.contains(query, ignoreCase = true)
                val phoneMatch = conv.contact.phoneNumber.contains(query, ignoreCase = true)
                val chatinIdMatch = conv.contact.chatinId.contains(query, ignoreCase = true)
                val lastMessageMatch = conv.lastMessage.contains(query, ignoreCase = true)
                val anyMessageMatch = messagesMap[conv.id]?.any { msg ->
                    msg.text.contains(query, ignoreCase = true) ||
                            (msg.fileName?.contains(query, ignoreCase = true) == true)
                } == true
                contactNameMatch || phoneMatch || chatinIdMatch || lastMessageMatch || anyMessageMatch
            }
            val matchesFilter = when (selectedFilter) {
                "Diarsipkan" -> conv.isArchived
                "Chatin Data" -> !conv.isArchived && conv.lastDeliveryChannel == DeliveryChannel.DATA_NETWORK
                "SMS Operator" -> !conv.isArchived && conv.lastDeliveryChannel == DeliveryChannel.SMS_FALLBACK
                "Belum Dibaca" -> !conv.isArchived && conv.unreadCount > 0
                else -> !conv.isArchived
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
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
                                isSelectionMode = false
                                selectedChatIds = emptySet()
                            }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Batal", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectedChatIds.size} dipilih",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val allFilteredIds = filteredConversations.map { it.id }.toSet()
                                selectedChatIds = if (selectedChatIds.size == allFilteredIds.size) emptySet() else allFilteredIds
                            }) {
                                Icon(imageVector = Icons.Default.SelectAll, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tandai Semua", fontSize = 12.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(onClick = {
                                onPinConversations(selectedChatIds)
                                isSelectionMode = false
                                selectedChatIds = emptySet()
                            }) {
                                Icon(imageVector = Icons.Default.PushPin, contentDescription = "Sematkan", tint = Color.White)
                            }
                            IconButton(onClick = {
                                showArchiveConfirmDialog = true
                            }) {
                                Icon(imageVector = Icons.Default.Archive, contentDescription = "Arsipkan", tint = Color(0xFF10B981))
                            }
                            IconButton(onClick = {
                                showDeleteConfirmDialog = true
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showContactPickerSheet = true },
                containerColor = Color.White,
                contentColor = Color(0xFF0D1B2A),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
                modifier = Modifier
                    .size(62.dp)
                    .border(2.dp, Color(0xFF00E5FF), CircleShape)
                    .testTag("new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Mencari Kontak atau Buat Chat Baru",
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF0284C7)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (searchQuery.isNotEmpty()) 1.5.dp else 1.dp,
                            color = if (searchQuery.isNotEmpty()) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari Kontak atau Isi Pesan",
                                tint = if (searchQuery.isNotEmpty()) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Cari nama kontak atau isi pesan...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_search_input")
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp).testTag("chat_search_clear_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Pencarian",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), CircleShape)
                            .testTag("sync_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "Sinkron Buku Telepon",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filteredConversations.isNotEmpty())
                                "Ditemukan ${filteredConversations.size} percakapan untuk \"${searchQuery.trim()}\""
                            else
                                "Tidak ada obrolan untuk \"${searchQuery.trim()}\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (filteredConversations.isNotEmpty()) Color(0xFF00E5FF) else Color(0xFFF59E0B)
                        )
                        Text(
                            text = "Reset",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { searchQuery = "" }
                                .padding(2.dp)
                        )
                    }
                }
            }

            if (searchQuery.isNotBlank() && filteredConversations.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF132035),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            onStartNewChatWithIdentifier(searchQuery)
                            searchQuery = ""
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF090D16))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mulai Chat ke '$searchQuery'",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Ketuk untuk langsung membuka ruang obrolan & panggilan",
                                fontSize = 11.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }

            // Filter Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterTabs) { tab ->
                    val isSelected = selectedFilter == tab
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
                        modifier = Modifier.clickable { selectedFilter = tab }
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Contacts Row
            if (contacts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts) { c ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { onStartNewChatWithIdentifier(c.phoneNumber) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(c.avatarColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(c.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = c.name.split(" ").firstOrNull() ?: c.name,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Long-press instruction
            Text(
                text = "💡 Tekan lama obrolan 1,5 detik untuk tandai semua, arsipkan, atau hapus",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            // Conversation List
            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF131A2B).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp),
                                    tint = Color(0xFF00E5FF)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Beranda Bersih & Siap Digunakan",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Kirim pesan terenkripsi E2EE atau panggil kontak terdaftar.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showContactPickerSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF090D16))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mulai Chat ke Nomor / Username", color = Color(0xFF090D16), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conv ->
                        val isSelected = selectedChatIds.contains(conv.id)
                        ConversationItemWith15sLongPress(
                            conversation = conv,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            matchingMessageSnippet = matchingMessagesSnippetMap[conv.id],
                            searchQuery = searchQuery,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedChatIds = if (isSelected) selectedChatIds - conv.id else selectedChatIds + conv.id
                                    if (selectedChatIds.isEmpty()) isSelectionMode = false
                                } else {
                                    onSelectChat(conv.id)
                                }
                            },
                            on15sLongPress = {
                                isSelectionMode = true
                                selectedChatIds = selectedChatIds + conv.id
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Konfirmasi Hapus Obrolan",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus ${selectedChatIds.size} percakapan yang dipilih?",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConversations(selectedChatIds)
                        showDeleteConfirmDialog = false
                        isSelectionMode = false
                        selectedChatIds = emptySet()
                        Toast.makeText(context, "Obrolan berhasil dihapus", Toast.LENGTH_SHORT).show()
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

    if (showArchiveConfirmDialog) {
        val isArchiving = selectedFilter != "Diarsipkan"
        AlertDialog(
            onDismissRequest = { showArchiveConfirmDialog = false },
            title = {
                Text(
                    text = if (isArchiving) "Konfirmasi Arsipkan Obrolan" else "Konfirmasi Buka Arsip",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = if (isArchiving)
                        "Pindahkan ${selectedChatIds.size} obrolan ke folder Arsip?"
                    else
                        "Kembalikan ${selectedChatIds.size} obrolan dari folder Arsip?",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onArchiveConversations(selectedChatIds, isArchiving)
                        showArchiveConfirmDialog = false
                        isSelectionMode = false
                        selectedChatIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(if (isArchiving) "Arsipkan" else "Buka Arsip", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirmDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131A2B),
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showContactPickerSheet) {
        NewChatContactPickerSheet(
            contacts = contacts,
            onSelectContact = { identifier ->
                onStartNewChatWithIdentifier(identifier)
                showContactPickerSheet = false
            },
            onAddNewContact = { name, phone, username ->
                onAddNewContact(name, phone, username)
            },
            onSyncDeviceContacts = onSyncContacts,
            onDismiss = { showContactPickerSheet = false }
        )
    }
}

@Composable
fun ConversationItemWith15sLongPress(
    conversation: ChatConversation,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    matchingMessageSnippet: String? = null,
    searchQuery: String = "",
    onClick: () -> Unit,
    on15sLongPress: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val contact = conversation.contact
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(conversation.lastMessageTimestamp) {
        timeFormat.format(Date(conversation.lastMessageTimestamp))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(conversation.id) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var isLongPressed = false
                    val timerJob = coroutineScope.launch {
                        delay(1500L) // 1.5 seconds long press
                        isLongPressed = true
                        on15sLongPress()
                    }
                    val up = waitForUpOrCancellation()
                    timerJob.cancel()
                    if (up != null && !isLongPressed) {
                        onClick()
                    }
                }
            }
            .testTag("chat_item_${conversation.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF)),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(contact.avatarColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                if (contact.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Disematkan",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val channelColor = when (conversation.lastDeliveryChannel) {
                            DeliveryChannel.DATA_NETWORK -> Color(0xFF00E5FF)
                            DeliveryChannel.SMS_FALLBACK -> Color(0xFFF59E0B)
                            DeliveryChannel.RCS_ADVANCED -> Color(0xFF7C4DFF)
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(channelColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = matchingMessageSnippet ?: conversation.lastMessage,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                fontSize = 10.sp,
                                color = Color(0xFF090D16),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
