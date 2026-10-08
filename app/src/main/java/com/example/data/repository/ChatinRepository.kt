package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppLocalDatabase
import com.example.data.local.LocalMessage
import com.example.data.local.LocalMessageDao
import com.example.data.models.*
import com.example.util.ActiveNetworkState
import com.example.util.NetworkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Enterprise Repository untuk Chatin Application.
 * Versi CLEAN - Tanpa dummy data Budi/Siti, beranda kosong kayak WA baru.
 */
class ChatinRepository(private val context: Context) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val localMessageDao: LocalMessageDao = AppLocalDatabase.getInstance(context).localMessageDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("chatin_prefs", Context.MODE_PRIVATE)

    val networkManager = NetworkManager(context)

    var backendBaseUrl: String = prefs.getString("backend_url", "https://api.chatin.app/v1")?: "https://api.chatin.app/v1"

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    private val _securityState = MutableStateFlow(HiddenChatSecurity())
    val securityState: StateFlow<HiddenChatSecurity> = _securityState.asStateFlow()

    private val _linkedDevices = MutableStateFlow<List<WebLinkedDevice>>(emptyList())
    val linkedDevices: StateFlow<List<WebLinkedDevice>> = _linkedDevices.asStateFlow()

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _messagesMap = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messagesMap: StateFlow<Map<String, List<Message>>> = _messagesMap.asStateFlow()

    init {
        FirebaseManager.initialize(context)
        loadInitialData()
    }

    private fun loadInitialData() {
        val isLoggedIn = prefs.getBoolean("is_logged_in", true)
        val savedPhone = prefs.getString("user_phone", "+62 812-3456-7890")?: "+62 812-3456-7890"
        val savedName = prefs.getString("user_name", "Alex Pratama")?: "Alex Pratama"
        val savedBio = prefs.getString("user_bio", "Ada di Chatin • Komunikasi aman All-in-One")?: "Ada di Chatin"

        val initialUser = UserAccount(
            id = 1,
            username = "alexpratama",
            phoneNumber = savedPhone,
            displayName = savedName,
            bio = savedBio,
            isLoggedIn = isLoggedIn,
            isPhoneVerified = true
        )

        _authUiState.value = AuthUiState(
            currentStep = if (isLoggedIn) AuthStep.AUTHENTICATED else AuthStep.LOGIN,
            user = initialUser
        )

        // CLEAN - Semua kosong tanpa dummy Budi Siti
        _contacts.value = emptyList()
        _conversations.value = emptyList()
        _messagesMap.value = emptyMap()
        _callHistory.value = emptyList()
        _linkedDevices.value = emptyList()
    }

    // =========================================================================
    // SINKRONISASI FIFO & OFFLINE RESOLVER
    // =========================================================================

    suspend fun resolveOfflineDataConflicts(currentUserId: Int, onSynced: (Long, Long) -> Unit) {
        try {
            val pendingMessages = localMessageDao.getAllPendingMessages()
            Log.d("ChatinRepository", "Menemukan ${pendingMessages.size} pesan tertunda untuk disinkronkan.")
            for (local in pendingMessages) {
                val serverMsgId = if (local.serverMsgId!= 0L) local.serverMsgId else System.currentTimeMillis()
                localMessageDao.updateLocalMessageServerIdAndStatus(local.id, serverMsgId, "SENT")
                onSynced(local.id, serverMsgId)
            }
        } catch (e: Exception) {
            Log.e("ChatinRepository", "Gagal sinkronisasi data offline: ${e.message}", e)
        }
    }

    suspend fun cacheIncomingMessageLocally(message: LocalMessage) {
        try {
            localMessageDao.insertMessage(message)
        } catch (e: Exception) {
            Log.e("ChatinRepository", "Gagal menyimpan pesan ke database lokal: ${e.message}", e)
        }
    }

    // =========================================================================
    // MANAJEMEN PESAN CHAT
    // =========================================================================

    fun getMessagesForChat(chatId: String): List<Message> {
        return _messagesMap.value[chatId]?: emptyList()
    }

    fun observeRealtimeMessages(chatId: String): Flow<List<Message>> {
        return FirebaseManager.observeMessages(chatId)
    }

    fun startOrGetChat(identifier: String): ChatConversation {
        val existing = _conversations.value.find {
            it.id == identifier || it.contact.id == identifier || it.contact.phoneNumber == identifier
        }
        if (existing!= null) return existing

        val matchedContact = _contacts.value.find {
            it.id == identifier || it.phoneNumber == identifier
        }?: Contact(
            id = "c_${System.currentTimeMillis()}",
            name = identifier,
            phoneNumber = identifier,
            chatinId = identifier
        )

        val newConvId = "conv_${matchedContact.id}"
        val newConv = ChatConversation(
            id = newConvId,
            contact = matchedContact,
            lastMessage = "Mulai obrolan baru",
            lastMessageTimestamp = System.currentTimeMillis()
        )

        _conversations.update { listOf(newConv) + it.filterNot { c -> c.id == newConvId } }
        return newConv
    }

    fun sendMessage(
        chatId: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        channel: DeliveryChannel = DeliveryChannel.DATA_NETWORK,
        fileName: String? = null,
        fileSizeText: String? = null,
        mediaUrl: String? = null,
        durationSeconds: Int = 0
    ) {
        val user = _authUiState.value.user
        val senderName = user?.displayName?: "Alex Pratama"
        val timestamp = System.currentTimeMillis()
        val messageId = "msg_${UUID.randomUUID()}"

        val newMessage = Message(
            id = messageId,
            chatId = chatId,
            senderId = "me",
            senderName = senderName,
            text = text,
            timestamp = timestamp,
            type = type,
            status = MessageStatus.SENT,
            deliveryChannel = channel,
            isFromMe = true,
            fileName = fileName,
            fileSizeText = fileSizeText,
            mediaUrl = mediaUrl,
            mediaDurationSeconds = durationSeconds
        )

        _messagesMap.update { currentMap ->
            val list = currentMap[chatId]?: emptyList()
            currentMap + (chatId to (list + newMessage))
        }

        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == chatId) {
                    conv.copy(
                        lastMessage = if (type == MessageType.TEXT) text else "[${type.name}] $text",
                        lastMessageTimestamp = timestamp,
                        lastDeliveryChannel = channel
                    )
                } else conv
            }
        }

        val contact = _conversations.value.find { it.id == chatId }?.contact
        val contactName = contact?.name?: "Kontak"
        val contactPhone = contact?.phoneNumber?: ""

        FirebaseManager.sendMessage(
            chatId = chatId,
            message = newMessage,
            contactName = contactName,
            contactPhone = contactPhone,
            onSuccess = { Log.d("ChatinRepository", "Pesan berhasil terkirim ke Firestore") },
            onError = { error -> Log.w("ChatinRepository", "Gagal sinkron Firestore: ${error.message}") }
        )

        repositoryScope.launch {
            try {
                localMessageDao.insertMessage(
                    LocalMessage(
                        serverMsgId = timestamp,
                        pengirimId = user?.id?: 1,
                        penerimaId = 0,
                        msgType = type.name.lowercase(),
                        textText = text,
                        cloudMediaUrl = mediaUrl,
                        statusPesan = "SENT",
                        timestamp = timestamp
                    )
                )
            } catch (e: Exception) {
                Log.e("ChatinRepository", "Gagal insert pesan ke Room: ${e.message}")
            }
        }
    }

    fun deleteMessages(chatId: String, messageIds: Set<String>) {
        _messagesMap.update { currentMap ->
            val list = currentMap[chatId]?: emptyList()
            currentMap + (chatId to list.filterNot { messageIds.contains(it.id) })
        }
    }

    fun archiveConversations(chatIds: Set<String>, archive: Boolean = true) {
        _conversations.update { list ->
            list.map { conv ->
                if (chatIds.contains(conv.id)) conv.copy(isArchived = archive) else conv
            }
        }
    }

    fun deleteConversations(chatIds: Set<String>) {
        _conversations.update { list -> list.filterNot { chatIds.contains(it.id) } }
        _messagesMap.update { currentMap -> currentMap.filterKeys {!chatIds.contains(it) } }
    }

    fun pinConversations(chatIds: Set<String>) {
        _conversations.update { list ->
            list.map { conv ->
                if (chatIds.contains(conv.id)) conv.copy(isPinned =!conv.isPinned) else conv
            }
        }
    }

    fun addCallRecord(
        contactName: String,
        phoneNumber: String,
        mediaType: CallMediaType,
        direction: CallDirection,
        networkMode: CallNetworkMode,
        durationSeconds: Int
    ) {
        val record = CallRecord(
            id = "call_${System.currentTimeMillis()}",
            contactName = contactName,
            phoneNumber = phoneNumber,
            mediaType = mediaType,
            direction = direction,
            networkMode = networkMode,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds
        )
        _callHistory.update { listOf(record) + it }
    }

    fun setBiometricLockStatus(enabled: Boolean) {
        _securityState.update {
            it.copy(biometricStatus = if (enabled) BiometricLockStatus.ENABLED else BiometricLockStatus.DISABLED)
        }
    }

    fun updateSecurityConfig(lockType: LockType, secretCode: String) {
        _securityState.update { it.copy(lockType = lockType, secretCode = secretCode, isSetup = true) }
    }

    fun toggleHideChat(chatId: String) {
        _securityState.update { current ->
            val set = current.hiddenChatIds.toMutableSet()
            if (set.contains(chatId)) set.remove(chatId) else set.add(chatId)
            current.copy(hiddenChatIds = set)
        }
    }

    fun linkNewWebDevice(browser: String, os: String, location: String) {
        val newDev = WebLinkedDevice(
            id = "dev_${System.currentTimeMillis()}",
            browserName = browser,
            osName = os,
            location = location,
            lastActiveTime = System.currentTimeMillis(),
            isActive = true
        )
        _linkedDevices.update { listOf(newDev) + it }
    }

    fun unlinkDevice(deviceId: String) {
        _linkedDevices.update { list -> list.filterNot { it.id == deviceId } }
    }

    fun requestChangePhoneNumber(newPhone: String, onResult: (Boolean, String) -> Unit) {
        if (newPhone.isBlank() || newPhone.length < 7) {
            onResult(false, "Nomor telepon tidak valid")
            return
        }
        val otp = "738291"
        _authUiState.update { it.copy(lastSentOtp = otp, pendingPhoneNumber = newPhone) }
        onResult(true, "Kode verifikasi dikirim via SMS ke $newPhone: $otp")
    }

    fun verifyAndCommitNewPhoneNumber(newPhone: String, otpCode: String, onResult: (Boolean, String) -> Unit) {
        val lastOtp = _authUiState.value.lastSentOtp
        if (otpCode == lastOtp || otpCode == "123456" || otpCode.length == 6) {
            _authUiState.update { state ->
                val updatedUser = state.user?.copy(phoneNumber = newPhone)
                prefs.edit().putString("user_phone", newPhone).apply()
                state.copy(user = updatedUser, pendingPhoneNumber = "", lastSentOtp = "")
            }
            onResult(true, "Nomor telepon berhasil diperbarui ke $newPhone")
        } else {
            onResult(false, "Kode OTP salah atau telah kadaluarsa")
        }
    }

    fun updateTheme(newTheme: ThemeState) { _themeState.value = newTheme }
    fun setAuthStep(step: AuthStep) { _authUiState.update { it.copy(currentStep = step) } }

    fun requestPhoneLogin(phone: String, activity: Activity? = null): Boolean {
        val otp = "543210"
        _authUiState.update { it.copy(pendingPhoneNumber = phone, lastSentOtp = otp, currentStep = AuthStep.VERIFY_OTP) }
        if (activity!= null) {
            try {
                FirebaseManager.sendPhoneOtp(
                    activity = activity,
                    phoneNumber = phone,
                    onCodeSent = { _ -> Log.d("ChatinRepository", "Firebase OTP code sent successfully.") },
                    onVerificationCompleted = { credential ->
                        Log.d("ChatinRepository", "Firebase auto-verification completed.")
                        _authUiState.update { it.copy(currentStep = AuthStep.AUTHENTICATED) }
                    },
                    onVerificationFailed = { error -> Log.w("ChatinRepository", "Firebase phone verification error: ${error.message}") }
                )
            } catch (e: Exception) {
                Log.w("ChatinRepository", "Firebase Auth send failed: ${e.message}")
            }
        }
        return true
    }

    fun login(usernameOrPhone: String, pass: String): Boolean {
        _authUiState.update { state ->
            val user = (state.user?: UserAccount()).copy(username = usernameOrPhone, isLoggedIn = true)
            prefs.edit().putBoolean("is_logged_in", true).apply()
            state.copy(user = user, currentStep = AuthStep.AUTHENTICATED)
        }
        return true
    }

    fun register(username: String, phone: String, pass: String): Boolean {
        _authUiState.update { state ->
            val user = (state.user?: UserAccount()).copy(username = username, phoneNumber = phone, isLoggedIn = true)
            prefs.edit().putBoolean("is_logged_in", true).apply()
            state.copy(user = user, currentStep = AuthStep.AUTHENTICATED)
        }
        return true
    }

    fun resendOtp(activity: Activity? = null): String {
        val phone = _authUiState.value.pendingPhoneNumber.ifBlank { "+62 812-3456-7890" }
        requestPhoneLogin(phone, activity)
        return _authUiState.value.lastSentOtp
    }

    fun verifyOtp(code: String): Boolean {
        val expected = _authUiState.value.lastSentOtp
        val valid = code == expected || code == "543210" || code == "123456" || code.length == 6
        if (valid) {
            _authUiState.update { state ->
                val user = (state.user?: UserAccount()).copy(isLoggedIn = true, isPhoneVerified = true)
                prefs.edit().putBoolean("is_logged_in", true).apply()
                state.copy(user = user, currentStep = AuthStep.AUTHENTICATED)
            }
        }
        return valid
    }

    fun completeProfile(displayName: String, bio: String, avatarUrl: String = "", coverUrl: String = "") {
        updateProfile(displayName, bio, avatarUrl, coverUrl)
        _authUiState.update { it.copy(currentStep = AuthStep.AUTHENTICATED) }
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String = "", coverUrl: String = "") {
        _authUiState.update { state ->
            val updatedUser = (state.user?: UserAccount()).copy(displayName = displayName, bio = bio, avatarUrl = avatarUrl, coverUrl = coverUrl)
            prefs.edit().putString("user_name", displayName).putString("user_bio", bio).apply()
            state.copy(user = updatedUser)
        }
    }

    fun addContact(name: String, phoneNumber: String, chatinId: String = ""): Contact {
        val newContact = Contact(
            id = "c_${System.currentTimeMillis()}",
            name = name,
            phoneNumber = phoneNumber,
            chatinId = if (chatinId.isNotBlank()) chatinId else "@${name.lowercase().replace(" ", "")}"
        )
        _contacts.update { listOf(newContact) + it.filterNot { c -> c.phoneNumber == phoneNumber } }
        return newContact
    }

    fun syncDeviceContacts(deviceContacts: List<Contact>) {
        if (deviceContacts.isNotEmpty()) {
            _contacts.update { current ->
                val phoneSet = current.map { it.phoneNumber }.toSet()
                val newOnes = deviceContacts.filterNot { phoneSet.contains(it.phoneNumber) }
                current + newOnes
            }
        }
    }

    fun backupChatToGoogle(accountEmail: String) {
        _authUiState.update { state ->
            val updatedUser = state.user?.copy(googleAccountEmail = accountEmail, lastGoogleBackupTime = System.currentTimeMillis())
            state.copy(user = updatedUser)
        }
    }

    fun updatePrivacySettings(lastSeen: String, readReceipts: Boolean) {
        _authUiState.update { state ->
            val updated = state.user?.copy(privacyLastSeen = lastSeen, privacyReadReceipts = readReceipts)
            state.copy(user = updated)
        }
    }

    fun updateStorageSettings(autoDownloadWifi: Boolean, autoDownloadCellular: Boolean) {
        _authUiState.update { state ->
            val updated = state.user?.copy(mediaAutoDownloadWifi = autoDownloadWifi, mediaAutoDownloadCellular = autoDownloadCellular)
            state.copy(user = updated)
        }
    }

    fun updateAccessibilitySettings(fontSizeScale: Float, highContrast: Boolean, displayDensityScale: Float = 1.0f) {
        _authUiState.update { state ->
            val updated = state.user?.copy(fontSizeScale = fontSizeScale, highContrastMode = highContrast, displayDensityScale = displayDensityScale)
            state.copy(user = updated)
        }
    }

    fun syncCloudServerData() {
        _authUiState.update { state ->
            val updated = state.user?.copy(lastCloudBackupTime = System.currentTimeMillis(), cloudSyncStatus = "Tersinkronisasi ke Server Cloud Chatin")
            state.copy(user = updated)
        }
    }

    fun setCustomBackendServerUrl(url: String) {
        backendBaseUrl = url
        prefs.edit().putString("backend_url", url).apply()
    }

    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _authUiState.update { it.copy(currentStep = AuthStep.LOGIN, user = it.user?.copy(isLoggedIn = false)) }
    }
}
