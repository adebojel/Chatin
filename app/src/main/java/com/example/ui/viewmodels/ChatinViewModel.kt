package com.example.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalMessage
import com.example.data.models.*
import com.example.data.repository.ChatinRepository
import com.example.util.ActiveNetworkState
import com.example.util.NetworkManager
import com.example.util.TelephonyHelper
import com.example.util.TorchManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActiveCallState(
    val isActive: Boolean = false,
    val contactName: String = "",
    val phoneNumber: String = "",
    val mediaType: CallMediaType = CallMediaType.VIDEO,
    val callOptions: VideoCallOptions = VideoCallOptions(),
    val durationSeconds: Int = 0,
    val networkMode: CallNetworkMode = CallNetworkMode.VOIP_WIFI
)

/**
 * ViewModel Master untuk Chatin Application.
 * Mengintegrasikan pemantauan jaringan realtime, pemicu sinkronisasi konflik data otomatis FIFO,
 * dan manajemen state reaktif Jetpack Compose.
 */
class ChatinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatinRepository(application.applicationContext)
    val torchManager = TorchManager(application.applicationContext)
    val networkManager: NetworkManager = repository.networkManager

    val contacts: StateFlow<List<Contact>> = repository.contacts
    val conversations: StateFlow<List<ChatConversation>> = repository.conversations
    val callHistory: StateFlow<List<CallRecord>> = repository.callHistory
    val themeState: StateFlow<ThemeState> = repository.themeState
    val isTorchOn: StateFlow<Boolean> = torchManager.isTorchOn
    val networkState: StateFlow<ActiveNetworkState> = networkManager.networkState
    val securityState: StateFlow<HiddenChatSecurity> = repository.securityState
    val linkedDevices: StateFlow<List<WebLinkedDevice>> = repository.linkedDevices
    val authUiState: StateFlow<AuthUiState> = repository.authUiState
    val messagesMap: StateFlow<Map<String, List<Message>>> = repository.messagesMap

    // Percakapan yang tampil di Beranda (menyaring obrolan rahasia yang disembunyikan)
    val visibleConversations: StateFlow<List<ChatConversation>> =
        combine(repository.conversations, repository.securityState) { convs, sec ->
            convs.filterNot { sec.hiddenChatIds.contains(it.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.conversations.value)

    val hiddenConversations: StateFlow<List<ChatConversation>> =
        combine(repository.conversations, repository.securityState) { convs, sec ->
            convs.filter { sec.hiddenChatIds.contains(it.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _currentChatMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentChatMessages: StateFlow<List<Message>> = _currentChatMessages.asStateFlow()

    private val _activeCallState = MutableStateFlow<ActiveCallState?>(null)
    val activeCallState: StateFlow<ActiveCallState?> = _activeCallState.asStateFlow()

    private val _isHiddenChatsUnlocked = MutableStateFlow(false)
    val isHiddenChatsUnlocked: StateFlow<Boolean> = _isHiddenChatsUnlocked.asStateFlow()

    private var callTimerJob: Job? = null

    init {
        // 1. Mengamati perubahan status sinyal internet secara berkala dan realtime (Anti-Crash Loop)
        viewModelScope.launch {
            networkState.collect { netState ->
                val currentUserId = authUiState.value.user?.id ?: 0

                if ((netState == ActiveNetworkState.WIFI_HD || netState == ActiveNetworkState.CELLULAR_DATA) && currentUserId != 0) {
                    Log.i("ANTI_BUG_NETWORK_MONITOR", "Perangkat kembali online. Memicu rekonsiliasi data FIFO...")

                    // Eksekusi penyelesaian konflik data di dalam background thread yang aman tanpa memblokir ketikan pengguna
                    repository.resolveOfflineDataConflicts(currentUserId) { localId, serverMsgId ->
                        Log.d("UI_SYNC_CALLBACK", "Pesan lokal nomor $localId berhasil disinkronkan ke server dengan ID #$serverMsgId.")
                    }
                }
            }
        }

        // 2. Mengamati obrolan yang sedang dipilih untuk memperbarui daftar gelembung pesan
        viewModelScope.launch {
            _selectedChatId.collect { id ->
                if (id != null) {
                    _currentChatMessages.value = repository.getMessagesForChat(id)
                }
            }
        }

        // 3. Mengamati pesan dari repository untuk menyegarkan tampilan obrolan aktif secara reaktif
        viewModelScope.launch {
            repository.messagesMap.collect { map ->
                val id = _selectedChatId.value
                if (id != null) {
                    _currentChatMessages.value = map[id] ?: emptyList()
                }
            }
        }

        // 4. Adaptasi bitrate VoIP otomatis terhadap perubahan kualitas jaringan secara dinamis
        viewModelScope.launch {
            networkState.collect { net ->
                _activeCallState.update { current ->
                    if (current != null) {
                        val newMode = when (net) {
                            ActiveNetworkState.WIFI_HD -> CallNetworkMode.VOIP_WIFI
                            ActiveNetworkState.CELLULAR_DATA -> CallNetworkMode.VOIP_CELLULAR
                            ActiveNetworkState.GSM_FALLBACK_ACTIVE -> CallNetworkMode.GSM_OPERATOR
                        }
                        val newBitrate = when (net) {
                            ActiveNetworkState.WIFI_HD -> 3200
                            ActiveNetworkState.CELLULAR_DATA -> 1400
                            ActiveNetworkState.GSM_FALLBACK_ACTIVE -> 64
                        }
                        current.copy(
                            networkMode = newMode,
                            callOptions = current.callOptions.copy(
                                networkMode = newMode,
                                bitrateKbps = newBitrate
                            )
                        )
                    } else null
                }
            }
        }
    }

    // Fungsi pengiriman offline aman jika user menekan kirim saat mode pesawat / tanpa sinyal
    fun sendMessageInOfflineMode(text: String, targetId: Int) {
        viewModelScope.launch {
            val currentUserId = authUiState.value.user?.id ?: 0
            val pendingMessage = LocalMessage(
                serverMsgId = System.currentTimeMillis(), // ID unik sementara
                pengirimId = currentUserId,
                penerimaId = targetId,
                msgType = "text",
                textText = text,
                cloudMediaUrl = null,
                mediaThumbnail = null,
                counter = 0,
                statusPesan = "PENDING", // Menandai pesan masuk ke dalam antrian lokal FIFO
                timestamp = System.currentTimeMillis()
            )
            repository.cacheIncomingMessageLocally(pendingMessage)
        }
    }

    fun selectChat(chatId: String) {
        _selectedChatId.value = chatId
        _currentChatMessages.value = repository.getMessagesForChat(chatId)
    }

    fun startOrGetChat(identifier: String) {
        val conv = repository.startOrGetChat(identifier)
        selectChat(conv.id)
    }

    fun closeChat() {
        _selectedChatId.value = null
    }

    fun sendMessage(
        text: String,
        type: MessageType = MessageType.TEXT,
        channel: DeliveryChannel = DeliveryChannel.DATA_NETWORK,
        fileName: String? = null,
        fileSizeText: String? = null,
        mediaUrl: String? = null,
        durationSeconds: Int = 0
    ) {
        val chatId = _selectedChatId.value ?: return
        repository.sendMessage(
            chatId = chatId,
            text = text,
            type = type,
            channel = channel,
            fileName = fileName,
            fileSizeText = fileSizeText,
            mediaUrl = mediaUrl,
            durationSeconds = durationSeconds
        )
        _currentChatMessages.value = repository.getMessagesForChat(chatId)
    }

    // Panggilan Suara & Video
    fun startCall(
        contactName: String,
        phoneNumber: String,
        mediaType: CallMediaType,
        forceGsm: Boolean = false
    ) {
        val net = networkState.value
        val initialMode = if (forceGsm || !net.isVoipCapable) {
            CallNetworkMode.GSM_OPERATOR
        } else if (net == ActiveNetworkState.WIFI_HD) {
            CallNetworkMode.VOIP_WIFI
        } else {
            CallNetworkMode.VOIP_CELLULAR
        }
        _activeCallState.value = ActiveCallState(
            isActive = true,
            contactName = contactName,
            phoneNumber = phoneNumber,
            mediaType = mediaType,
            durationSeconds = 0,
            networkMode = initialMode,
            callOptions = VideoCallOptions(
                networkMode = initialMode,
                bitrateKbps = if (initialMode == CallNetworkMode.VOIP_WIFI) 3200 else 1500
            )
        )
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCallState.update { current ->
                    current?.copy(durationSeconds = current.durationSeconds + 1)
                }
            }
        }
    }

    fun endCall() {
        val current = _activeCallState.value
        if (current != null) {
            repository.addCallRecord(
                contactName = current.contactName,
                phoneNumber = current.phoneNumber,
                mediaType = current.mediaType,
                direction = CallDirection.OUTGOING,
                networkMode = current.networkMode,
                durationSeconds = current.durationSeconds
            )
        }
        callTimerJob?.cancel()
        callTimerJob = null
        _activeCallState.value = null
    }

    fun updateCallOptions(transform: (VideoCallOptions) -> VideoCallOptions) {
        _activeCallState.update { current ->
            current?.copy(callOptions = transform(current.callOptions))
        }
    }

    fun setVoiceChanger(effect: VoiceChangerEffect) {
        updateCallOptions { it.copy(voiceChanger = effect) }
    }

    fun setGreenScreenWallpaper(uriString: String) {
        _activeCallState.update { current ->
            current?.copy(
                callOptions = current.callOptions.copy(
                    backgroundMode = VirtualBackgroundMode.GREEN_SCREEN_GALLERY,
                    greenScreenCustomUri = uriString
                )
            )
        }
    }

    fun toggleFlashlight() {
        torchManager.toggleTorch()
        _activeCallState.update { current ->
            current?.copy(
                callOptions = current.callOptions.copy(
                    isFlashlightOn = torchManager.isTorchOn.value
                )
            )
        }
    }

    fun switchToGsmDialer() {
        val current = _activeCallState.value ?: return
        TelephonyHelper.dialPhoneNumber(getApplication(), current.phoneNumber)
        endCall()
    }

    fun dialContactGsm(phoneNumber: String) {
        TelephonyHelper.dialPhoneNumber(getApplication(), phoneNumber)
    }

    fun sendSmsFallback(phoneNumber: String, text: String = "") {
        TelephonyHelper.sendSmsFallback(getApplication(), phoneNumber, text)
    }

    // Ubah Nomor Telepon Asli (2-Tahap OTP)
    private val _isPhoneChanging = MutableStateFlow(false)
    val isPhoneChanging: StateFlow<Boolean> = _isPhoneChanging.asStateFlow()
    private var pendingNewPhoneNumber: String = ""

    fun setBiometricLockStatus(enabled: Boolean) {
        repository.setBiometricLockStatus(enabled)
    }

    fun initChangePhoneNumber(newPhone: String, onResult: (Boolean, String) -> Unit) {
        pendingNewPhoneNumber = newPhone
        viewModelScope.launch {
            repository.requestChangePhoneNumber(newPhone) { success, msg ->
                if (success) {
                    _isPhoneChanging.value = true
                }
                onResult(success, msg)
            }
        }
    }

    fun submitOtpVerificationCode(otpCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.verifyAndCommitNewPhoneNumber(pendingNewPhoneNumber, otpCode) { success, msg ->
                if (success) {
                    _isPhoneChanging.value = false
                    pendingNewPhoneNumber = ""
                }
                onResult(success, msg)
            }
        }
    }

    fun requestChangePhoneNumber(newPhone: String, onResult: (Boolean, String) -> Unit) {
        initChangePhoneNumber(newPhone, onResult)
    }

    fun verifyAndCommitNewPhoneNumber(newPhone: String, otpCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.verifyAndCommitNewPhoneNumber(newPhone, otpCode, onResult)
        }
    }

    // Manajemen Tema & Wallpaper
    fun setThemeMode(mode: AppThemeMode) {
        repository.updateTheme(themeState.value.copy(themeMode = mode))
    }

    fun setBackgroundPreset(preset: BackgroundPreset) {
        repository.updateTheme(themeState.value.copy(backgroundPreset = preset))
    }

    fun generateAiTheme(prompt: String) {
        repository.updateTheme(
            themeState.value.copy(
                aiPrompt = prompt,
                backgroundPreset = BackgroundPreset.AI_GENERATED
            )
        )
    }

    fun setGalleryWallpaper(uriString: String) {
        repository.updateTheme(
            themeState.value.copy(
                backgroundPreset = BackgroundPreset.CUSTOM_GALLERY,
                customGalleryUri = uriString
            )
        )
    }

    // Ruang Obrolan Rahasia & Biometrik
    fun verifyUnlockCode(input: String): Boolean {
        val currentSecret = securityState.value.secretCode
        val isCorrect = input.trim() == currentSecret.trim()
        if (isCorrect) {
            _isHiddenChatsUnlocked.value = true
        }
        return isCorrect
    }

    fun unlockHiddenChatsBiometric() {
        _isHiddenChatsUnlocked.value = true
    }

    fun lockHiddenChats() {
        _isHiddenChatsUnlocked.value = false
    }

    fun updateSecurityConfig(lockType: LockType, secretCode: String) {
        repository.updateSecurityConfig(lockType, secretCode)
    }

    fun toggleHideChat(chatId: String) {
        repository.toggleHideChat(chatId)
    }

    // Perangkat Web Tertaut
    fun linkNewWebDevice(browser: String, os: String, location: String) {
        repository.linkNewWebDevice(browser, os, location)
    }

    fun unlinkDevice(deviceId: String) {
        repository.unlinkDevice(deviceId)
    }

    // Otentikasi Pengguna
    fun setAuthStep(step: AuthStep) {
        repository.setAuthStep(step)
    }

    fun login(usernameOrPhone: String, pass: String): Boolean {
        return repository.login(usernameOrPhone, pass)
    }

    fun register(username: String, phone: String, pass: String): Boolean {
        return repository.register(username, phone, pass)
    }

    fun resendOtp(): String {
        return repository.resendOtp()
    }

    fun verifyOtp(code: String): Boolean {
        return repository.verifyOtp(code)
    }

    fun completeProfile(displayName: String, bio: String, avatarUrl: String = "", coverUrl: String = "") {
        repository.completeProfile(displayName, bio, avatarUrl, coverUrl)
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String = "", coverUrl: String = "") {
        repository.updateProfile(displayName, bio, avatarUrl, coverUrl)
    }

    fun addContact(name: String, phoneNumber: String, chatinId: String = ""): Contact {
        return repository.addContact(name, phoneNumber, chatinId)
    }

    fun backupChatToGoogle(accountEmail: String) {
        repository.backupChatToGoogle(accountEmail)
    }

    fun updatePrivacySettings(lastSeen: String, readReceipts: Boolean) {
        repository.updatePrivacySettings(lastSeen, readReceipts)
    }

    fun updateStorageSettings(autoDownloadWifi: Boolean, autoDownloadCellular: Boolean) {
        repository.updateStorageSettings(autoDownloadWifi, autoDownloadCellular)
    }

    fun updateAccessibilitySettings(fontSizeScale: Float, highContrast: Boolean, displayDensityScale: Float = 1.0f) {
        repository.updateAccessibilitySettings(fontSizeScale, highContrast, displayDensityScale)
    }

    fun updateDisplaySettings(fontSizeScale: Float, displayDensityScale: Float) {
        val curHighContrast = authUiState.value.user?.highContrastMode ?: false
        repository.updateAccessibilitySettings(fontSizeScale, curHighContrast, displayDensityScale)
    }

    fun syncCloudServerData() {
        repository.syncCloudServerData()
    }

    fun logout() {
        repository.logout()
    }

    fun syncDeviceContacts(context: android.content.Context) {
        val deviceContacts = com.example.util.DeviceContactsHelper.loadDeviceContacts(context)
        repository.syncDeviceContacts(deviceContacts)
    }

    fun sendDirectSms(
        context: android.content.Context,
        chatId: String,
        phoneNumber: String,
        text: String
    ) {
        com.example.util.TelephonyHelper.sendDirectSms(context, phoneNumber, text)
        sendMessage(
            text = text,
            type = MessageType.TEXT,
            channel = DeliveryChannel.GSM_SMS
        )
    }

    fun archiveConversations(chatIds: Set<String>, archive: Boolean = true) {
        repository.archiveConversations(chatIds, archive)
    }

    fun deleteConversations(chatIds: Set<String>) {
        repository.deleteConversations(chatIds)
    }

    fun pinConversations(chatIds: Set<String>) {
        repository.pinConversations(chatIds)
    }

    fun deleteMessages(chatId: String, messageIds: Set<String>) {
        repository.deleteMessages(chatId, messageIds)
        _currentChatMessages.value = repository.getMessagesForChat(chatId)
    }

    override fun onCleared() {
        super.onCleared()
        callTimerJob?.cancel()
    }
}
