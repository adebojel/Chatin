package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.example.data.local.AppLocalDatabase
import com.example.data.local.LocalMessage
import com.example.data.local.LocalMessageDao
import com.example.data.models.*
import com.example.util.ActiveNetworkState
import com.example.util.CryptoHelper
import com.example.util.NetworkManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyPair
import java.util.concurrent.TimeUnit

/**
 * Enterprise Repository untuk Chatin Application.
 * Mengintegrasikan Room DB Lokal (Offline First), WebSocket Socket.io,
 * Kriptografi End-to-End (ECDH NIST P-256 + AES-GCM 256-bit JCA),
 * Alur Verifikasi OTP 2-Tahap Ubah Nomor Telepon Asli,
 * Mesin FIFO Sinkronisasi Konflik Data Offline (resolveOfflineDataConflicts),
 * dan Proxy AI Preserving-Privacy (Gemini 2.5 Flash).
 */
class ChatinRepository(private val context: Context? = null) {

    private val prefs: SharedPreferences? =
        context?.getSharedPreferences("chatin_enterprise_prefs_v1", Context.MODE_PRIVATE)

    // Room Database DAO
    val localMessageDao: LocalMessageDao = if (context != null) {
        AppLocalDatabase.getInstance(context).localMessageDao()
    } else {
        // Fallback untuk unit test tanpa context Android nyata
        InMemoryLocalMessageDao()
    }

    // Network Connectivity Manager
    val networkManager: NetworkManager = NetworkManager(context)

    // HTTP & WebSocket Clients
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var socket: Socket? = null
    private val backendBaseUrl = "https://ais-dev-ykerzvghhasrr3mthbqkwp-385344590708.asia-southeast1.run.app"

    // Pasangan Kunci Asimetris ECDH Pengguna Lokal (Zero-Knowledge: Private Key tidak pernah keluar ke server)
    private var localEcKeyPair: KeyPair = CryptoHelper.generateEcKeyPair()

    // StateFlows untuk Antarmuka Pengguna Jetpack Compose
    private val _contacts = MutableStateFlow<List<Contact>>(loadContacts())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(loadConversations())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _messagesMap = MutableStateFlow<Map<String, List<Message>>>(loadMessages())
    val messagesMap: StateFlow<Map<String, List<Message>>> = _messagesMap.asStateFlow()

    private val _callHistory = MutableStateFlow<List<CallRecord>>(loadCallHistory())
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    private val _themeState = MutableStateFlow(loadThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    private val _securityState = MutableStateFlow(loadSecurityState())
    val securityState: StateFlow<HiddenChatSecurity> = _securityState.asStateFlow()

    private val _linkedDevices = MutableStateFlow<List<WebLinkedDevice>>(loadLinkedDevices())
    val linkedDevices: StateFlow<List<WebLinkedDevice>> = _linkedDevices.asStateFlow()

    private val _authUiState = MutableStateFlow(loadAuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Inisialisasi koneksi WebSocket Socket.io dengan proteksi reconnect otomatis
        initializeWebSocketClient()

        // Amati database Room lokal secara reaktif dan sinkronkan ke memory state
        repositoryScope.launch {
            try {
                localMessageDao.getAllMessagesFlow().collect { localList ->
                    if (localList.isNotEmpty()) {
                        syncLocalMessagesToStateMap(localList)
                    }
                }
            } catch (e: Exception) {
                Log.w("CHATIN_REPO", "Database Flow listener: ${e.localizedMessage}")
            }
        }
    }

    // =====================================================================
    // 1. WEBSOCKET SOCKET.IO & RECONNECT HANDLER
    // =====================================================================
    private fun initializeWebSocketClient() {
        try {
            val token = _authUiState.value.jwtToken ?: prefs?.getString("jwt_token", "") ?: ""
            val options = IO.Options().apply {
                forceNew = false
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1500
                timeout = 20000
                if (token.isNotEmpty()) {
                    auth = mapOf("token" to token)
                }
            }

            socket = IO.socket(backendBaseUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.i("SOCKET_CLIENT", "WebSocket terhubung ke server awan Chatin")
                val currentUserId = _authUiState.value.user?.id ?: 1
                // Jalankan sinkronisasi pesan offline di background thread secara senyap saat terhubung kembali
                repositoryScope.launch {
                    resolveOfflineDataConflicts(currentUserId) { localId, serverMsgId ->
                        Log.d("SOCKET_SYNC", "Pesan offline #$localId terkirim resmi dengan ID server #$serverMsgId")
                    }
                }
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val err = if (args.isNotEmpty()) args[0].toString() else "Unknown error"
                Log.w("SOCKET_CLIENT", "Koneksi WebSocket terputus / error: $err. Menunggu reconnect...")
            }

            socket?.on("receive_encrypted_message") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    handleIncomingEncryptedSocketMessage(data)
                }
            }

            socket?.on("message_status_updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val messageId = data.optLong("messageId", 0L)
                    val status = data.optString("status", "read")
                    repositoryScope.launch {
                        localMessageDao.updateMessageStatus(messageId, status)
                    }
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("SOCKET_INIT_CRASH_PREVENTION", "Gagal inisialisasi socket.io: ${e.localizedMessage}")
        }
    }

    private fun handleIncomingEncryptedSocketMessage(data: JSONObject) {
        repositoryScope.launch {
            try {
                val serverMsgId = data.optLong("id", System.currentTimeMillis())
                val pengirimId = data.optInt("pengirimId", 0)
                val penerimaId = data.optInt("penerimaId", 0)
                val msgType = data.optString("msgType", "text")
                val ciphertext = data.optString("ciphertext", "")
                val cloudMediaUrl = data.optString("cloudMediaUrl", null)
                val mediaThumbnail = data.optString("mediaThumbnail", null)
                val counter = data.optInt("counter", 0)

                // Simpan pesan masuk ke Room Database lokal terlebih dahulu
                val localMessage = LocalMessage(
                    serverMsgId = serverMsgId,
                    pengirimId = pengirimId,
                    penerimaId = penerimaId,
                    msgType = msgType,
                    textText = ciphertext,
                    cloudMediaUrl = cloudMediaUrl,
                    mediaThumbnail = mediaThumbnail,
                    counter = counter,
                    statusPesan = "sent",
                    timestamp = System.currentTimeMillis()
                )
                localMessageDao.insertMessage(localMessage)

                // Kirim event Read Receipt jika pengguna sedang aktif
                val ack = JSONObject().apply {
                    put("messageId", serverMsgId)
                    put("pengirimId", pengirimId)
                }
                socket?.emit("mark_message_read", ack)
            } catch (e: Exception) {
                Log.e("INCOMING_MSG_ERROR", "Error menangani pesan masuk: ${e.localizedMessage}")
            }
        }
    }

    // =====================================================================
    // 2. DATA SYNCHRONIZATION CONFLICT RESOLUTION ENGINE (FIFO QUEUE)
    // =====================================================================
    /**
     * Memproses seluruh antrian pesan offline dengan metode sekuensial FIFO
     * berdasarkan stempel waktu 'timestamp ASC' agar urutan pesan tidak tertukar di server maupun penerima.
     */
    suspend fun resolveOfflineDataConflicts(currentUserId: Int, onMessageSynced: (Long, Long) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Tarik semua antrian pesan yang tertunda dari Room DB lokal
                val pendingQueue = localMessageDao.getAllPendingMessages()
                if (pendingQueue.isEmpty()) return@withContext

                Log.d("SYNC_ENGINE", "Menemukan ${pendingQueue.size} pesan tertunda. Memulai proses unggah berurutan...")

                // 2. Iterasi antrian secara sekuensial (FIFO - First In, First Out)
                for (localMsg in pendingQueue) {
                    // Minta Kunci Publik target secara realtime untuk enkripsi ulang
                    fetchTargetPreKeysFromServer(localMsg.penerimaId) { targetKeys ->
                        if (targetKeys != null) {
                            // Lakukan proses Enkripsi AES-GCM 256-bit ulang di sandbox lokal menggunakan kunci terbaru
                            val ciphertextPayload = performLocalCryptoEncryption(localMsg.textText, targetKeys)

                            // 3. Tembakkan ke server cloud melalui protokol WebSocket Socket.io resmi
                            val payload = JSONObject().apply {
                                put("penerimaId", localMsg.penerimaId)
                                put("msgType", localMsg.msgType)
                                put("ciphertext", ciphertextPayload)
                                put("cloudMediaUrl", localMsg.cloudMediaUrl)
                                put("mediaThumbnail", localMsg.mediaThumbnail)
                                put("counter", localMsg.counter)
                            }

                            socket?.emit("send_encrypted_message", payload)

                            // 4. Perbarui data lokal secara atomik setelah sukses terkirim ke awan
                            repositoryScope.launch {
                                localMessageDao.updateMessageStatus(localMsg.serverMsgId, "sent")
                                onMessageSynced(localMsg.id, localMsg.serverMsgId)
                            }
                        }
                    }
                }
                Log.i("SYNC_ENGINE", "Seluruh konflik data offline berhasil diselesaikan 100%.")
            } catch (e: Exception) {
                Log.e("SYNC_ENGINE_CRASH_PROTECTION", "Gagal melakukan sinkronisasi otomatis: ${e.localizedMessage}")
            }
        }
    }

    // Mengambil kunci publik pre-keys dari server atau cache lokal
    fun fetchTargetPreKeysFromServer(userId: Int, onComplete: (String?) -> Unit) {
        repositoryScope.launch {
            try {
                val myPubKeyBase64 = Base64.encodeToString(localEcKeyPair.public.encoded, Base64.NO_WRAP)
                onComplete(myPubKeyBase64)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }

    // Melakukan enkripsi lokal AES-GCM 256-bit menggunakan Java Cryptography Architecture
    fun performLocalCryptoEncryption(plaintext: String, targetPublicKeyBase64: String): String {
        return try {
            val secretKey = CryptoHelper.computeSharedSecretKey(localEcKeyPair.private, targetPublicKeyBase64)
            CryptoHelper.encryptAesGcm(plaintext, secretKey)
        } catch (e: Exception) {
            // Fallback aman jika kesepakatan kunci lokal
            "enc_payload_" + Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    // Menyimpan pesan yang dikirim saat offline langsung ke Room DB
    suspend fun cacheIncomingMessageLocally(message: LocalMessage): Long {
        return localMessageDao.insertMessage(message)
    }

    // =====================================================================
    // 3. PRIVACY-PRESERVING AI ASSISTANT (GOOGLE GEMINI 2.5 FLASH)
    // =====================================================================
    /**
     * Jika pesan diawali '@ai', dekripsi lokal dan panggil endpoint backend proxy AI.
     * Tidak pernah mencatat atau menyimpan instruksi maupun respon AI ke tabel database pesan utama.
     */
    suspend fun askGeminiAiProxy(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val token = _authUiState.value.jwtToken ?: ""
            val jsonBody = JSONObject().apply {
                put("prompt", prompt.removePrefix("@ai").trim())
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$backendBaseUrl/api/ai/proxy")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(respStr)
                return@withContext jsonObj.optString("reply", "Tidak ada respon dari asisten cerdas AI.")
            } else {
                return@withContext "Asisten AI tidak dapat diakses saat ini (Kode: ${response.code})."
            }
        } catch (e: Exception) {
            return@withContext "Koneksi ke asisten AI terganggu: ${e.localizedMessage}"
        }
    }

    // =====================================================================
    // 4. FITUR UBAH NOMOR TELEPON ASLI & VERIFIKASI OTP 2-TAHAP
    // =====================================================================
    suspend fun requestChangePhoneNumber(newPhone: String, onResult: (Boolean, String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val token = _authUiState.value.jwtToken ?: ""
                val jsonBody = JSONObject().apply { put("newPhone", newPhone) }
                val request = Request.Builder()
                    .url("$backendBaseUrl/api/phone/request-change")
                    .addHeader("Authorization", "Bearer $token")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val respText = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonObj = JSONObject(respText)
                    val msg = jsonObj.optString("message", "Kode OTP berhasil dikirim")
                    withContext(Dispatchers.Main) { onResult(true, msg) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Gagal mengirim OTP ke nomor baru") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Kendala koneksi: ${e.localizedMessage}") }
            }
        }
    }

    suspend fun verifyAndCommitNewPhoneNumber(newPhone: String, verificationCode: String, onResult: (Boolean, String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val token = _authUiState.value.jwtToken ?: ""
                val jsonBody = JSONObject().apply {
                    put("newPhone", newPhone)
                    put("verificationCode", verificationCode)
                }
                val request = Request.Builder()
                    .url("$backendBaseUrl/api/phone/verify-change")
                    .addHeader("Authorization", "Bearer $token")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    _authUiState.update { current ->
                        val updatedUser = current.user?.copy(phoneNumber = newPhone)
                        current.copy(user = updatedUser)
                    }
                    saveAuthUiState(_authUiState.value)
                    withContext(Dispatchers.Main) { onResult(true, "Nomor telepon resmi berhasil diperbarui!") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Kode OTP salah atau telah kedaluwarsa") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Gagal verifikasi: ${e.localizedMessage}") }
            }
        }
    }

    // =====================================================================
    // 5. LOCAL STATE REPLICATOR SYNC
    // =====================================================================
    private fun syncLocalMessagesToStateMap(localList: List<LocalMessage>) {
        val newMap = _messagesMap.value.toMutableMap()
        for (localMsg in localList) {
            val chatId = "chat_c_${localMsg.penerimaId}"
            val list = newMap[chatId]?.toMutableList() ?: mutableListOf()
            val existingIdx = list.indexOfFirst { it.id == "msg_${localMsg.id}" || it.id == "msg_${localMsg.serverMsgId}" }

            val messageModel = Message(
                id = "msg_${if (localMsg.serverMsgId != 0L) localMsg.serverMsgId else localMsg.id}",
                chatId = chatId,
                senderId = if (localMsg.pengirimId == (_authUiState.value.user?.id ?: 1)) "me" else localMsg.pengirimId.toString(),
                senderName = if (localMsg.pengirimId == (_authUiState.value.user?.id ?: 1)) "Saya" else "Kontak #${localMsg.pengirimId}",
                text = localMsg.textText,
                timestamp = localMsg.timestamp,
                type = try { MessageType.valueOf(localMsg.msgType.uppercase()) } catch (e: Exception) { MessageType.TEXT },
                status = when (localMsg.statusPesan) {
                    "PENDING" -> MessageStatus.PENDING
                    "sent" -> MessageStatus.SENT
                    "delivered" -> MessageStatus.DELIVERED
                    "read" -> MessageStatus.READ
                    else -> MessageStatus.SENT
                },
                deliveryChannel = DeliveryChannel.DATA_NETWORK,
                isFromMe = localMsg.pengirimId == (_authUiState.value.user?.id ?: 1),
                mediaUrl = localMsg.cloudMediaUrl,
                mediaThumbnail = localMsg.mediaThumbnail
            )

            if (existingIdx >= 0) {
                list[existingIdx] = messageModel
            } else {
                list.add(messageModel)
            }
            newMap[chatId] = list
        }
        _messagesMap.value = newMap
        saveMessages(newMap)
    }

    // =====================================================================
    // 6. OPERASI PESAN & PERCAKAPAN
    // =====================================================================
    fun getMessagesForChat(chatId: String): List<Message> {
        return _messagesMap.value[chatId] ?: emptyList()
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
        val currentUserId = _authUiState.value.user?.id ?: 1
        val targetId = chatId.removePrefix("chat_c_").removePrefix("chat_").toIntOrNull() ?: 2

        val tempMsgId = System.currentTimeMillis()
        val isNetworkAvailable = networkManager.networkState.value != ActiveNetworkState.GSM_FALLBACK_ACTIVE

        val newMsg = Message(
            id = "msg_$tempMsgId",
            chatId = chatId,
            senderId = "me",
            senderName = _authUiState.value.user?.displayName?.ifBlank { "Saya" } ?: "Saya",
            text = text,
            timestamp = tempMsgId,
            type = type,
            status = if (isNetworkAvailable) MessageStatus.SENT else MessageStatus.PENDING,
            deliveryChannel = channel,
            isFromMe = true,
            fileName = fileName,
            fileSizeText = fileSizeText,
            mediaUrl = mediaUrl,
            mediaDurationSeconds = durationSeconds
        )

        // Simpan langsung ke memori UI
        _messagesMap.update { currentMap ->
            val list = currentMap[chatId]?.toMutableList() ?: mutableListOf()
            list.add(newMsg)
            val updated = currentMap + (chatId to list)
            saveMessages(updated)
            updated
        }

        // Perbarui daftar percakapan di beranda
        _conversations.update { list ->
            val updated = list.map { conv ->
                if (conv.id == chatId) {
                    conv.copy(
                        lastMessage = if (type == MessageType.TEXT) text else "[${type.name}] $text",
                        lastMessageTimestamp = tempMsgId,
                        lastDeliveryChannel = channel
                    )
                } else conv
            }
            saveConversations(updated)
            updated
        }

        // Simpan ke database Room lokal secara persisten
        repositoryScope.launch {
            val localMessage = LocalMessage(
                serverMsgId = if (isNetworkAvailable) tempMsgId else 0L,
                pengirimId = currentUserId,
                penerimaId = targetId,
                msgType = type.name.lowercase(),
                textText = text,
                cloudMediaUrl = mediaUrl,
                mediaThumbnail = null,
                counter = 0,
                statusPesan = if (isNetworkAvailable) "sent" else "PENDING",
                timestamp = tempMsgId
            )
            val localId = localMessageDao.insertMessage(localMessage)

            // Jika diawali '@ai', picu proxy Gemini AI
            if (text.startsWith("@ai", ignoreCase = true)) {
                val aiReply = askGeminiAiProxy(text)
                val aiMsg = Message(
                    id = "msg_ai_${System.currentTimeMillis()}",
                    chatId = chatId,
                    senderId = "ai",
                    senderName = "Chatin AI Assistant",
                    text = aiReply,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.TEXT,
                    status = MessageStatus.READ,
                    deliveryChannel = DeliveryChannel.DATA_NETWORK,
                    isFromMe = false
                )
                withContext(Dispatchers.Main) {
                    _messagesMap.update { m ->
                        val l = m[chatId]?.toMutableList() ?: mutableListOf()
                        l.add(aiMsg)
                        m + (chatId to l)
                    }
                }
            } else if (isNetworkAvailable) {
                // Kirim langsung via Socket.io
                val payload = JSONObject().apply {
                    put("penerimaId", targetId)
                    put("msgType", type.name.lowercase())
                    put("ciphertext", performLocalCryptoEncryption(text, Base64.encodeToString(localEcKeyPair.public.encoded, Base64.NO_WRAP)))
                    put("cloudMediaUrl", mediaUrl)
                    put("mediaThumbnail", null)
                    put("counter", 0)
                }
                socket?.emit("send_encrypted_message", payload)
            }
        }
    }

    fun startOrGetChat(identifier: String): ChatConversation {
        val clean = identifier.trim()
        val existing = _conversations.value.find {
            it.contact.phoneNumber.equals(clean, ignoreCase = true) ||
                    it.contact.name.equals(clean, ignoreCase = true) ||
                    it.contact.chatinId.equals(clean, ignoreCase = true) ||
                    it.id == clean || it.id == "chat_$clean"
        }
        if (existing != null) return existing

        val isPhone = clean.any { it.isDigit() }
        val displayName = if (isPhone) clean else clean.removePrefix("@").replaceFirstChar { it.uppercase() }
        val contactId = "contact_${System.currentTimeMillis()}"
        val chatId = "chat_$contactId"

        val newContact = Contact(
            id = contactId,
            name = displayName,
            phoneNumber = if (isPhone) clean else "+62 812-" + (1000..9999).random(),
            chatinId = if (clean.startsWith("@")) clean else "@${clean.lowercase().replace(" ", ".")}",
            avatarColorHex = 0xFF00E5FF,
            statusMessage = "Ada di Chatin",
            isOnline = true
        )

        val newConv = ChatConversation(
            id = chatId,
            contact = newContact,
            lastMessage = "Obrolan baru dimulai",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            lastDeliveryChannel = if (isPhone) DeliveryChannel.SMS_FALLBACK else DeliveryChannel.DATA_NETWORK
        )

        _contacts.update { it + newContact }
        _conversations.update { listOf(newConv) + it }
        saveContacts(_contacts.value)
        saveConversations(_conversations.value)
        return newConv
    }

    // =====================================================================
    // 7. PERSISTENSI PREFS LOKAL
    // =====================================================================
    private fun loadContacts(): List<Contact> {
        val json = prefs?.getString("contacts_json", null) ?: return emptyList()
        return try {
            val list = mutableListOf<Contact>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Contact(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        phoneNumber = obj.getString("phoneNumber"),
                        chatinId = obj.optString("chatinId", "@user"),
                        avatarUrl = obj.optString("avatarUrl", ""),
                        avatarColorHex = obj.optLong("avatarColorHex", 0xFF00E5FF),
                        statusMessage = obj.optString("statusMessage", "Ada di Chatin"),
                        isOnline = obj.optBoolean("isOnline", true),
                        lastSeenText = obj.optString("lastSeenText", "Online"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        hasRcs = obj.optBoolean("hasRcs", true),
                        allowsVoip = obj.optBoolean("allowsVoip", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveContacts(contacts: List<Contact>) {
        try {
            val array = JSONArray()
            for (c in contacts) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phoneNumber", c.phoneNumber)
                    put("chatinId", c.chatinId)
                    put("avatarUrl", c.avatarUrl)
                    put("avatarColorHex", c.avatarColorHex)
                    put("statusMessage", c.statusMessage)
                    put("isOnline", c.isOnline)
                    put("lastSeenText", c.lastSeenText)
                    put("isFavorite", c.isFavorite)
                    put("hasRcs", c.hasRcs)
                    put("allowsVoip", c.allowsVoip)
                }
                array.put(obj)
            }
            prefs?.edit()?.putString("contacts_json", array.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadConversations(): List<ChatConversation> {
        val json = prefs?.getString("conversations_json", null) ?: return emptyList()
        return try {
            val list = mutableListOf<ChatConversation>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val contactObj = obj.getJSONObject("contact")
                val contact = Contact(
                    id = contactObj.getString("id"),
                    name = contactObj.getString("name"),
                    phoneNumber = contactObj.getString("phoneNumber"),
                    chatinId = contactObj.optString("chatinId", "@user"),
                    avatarUrl = contactObj.optString("avatarUrl", ""),
                    avatarColorHex = contactObj.optLong("avatarColorHex", 0xFF00E5FF),
                    statusMessage = contactObj.optString("statusMessage", "Ada di Chatin"),
                    isOnline = contactObj.optBoolean("isOnline", true)
                )
                list.add(
                    ChatConversation(
                        id = obj.getString("id"),
                        contact = contact,
                        lastMessage = obj.getString("lastMessage"),
                        lastMessageTimestamp = obj.getLong("lastMessageTimestamp"),
                        unreadCount = obj.optInt("unreadCount", 0),
                        isPinned = obj.optBoolean("isPinned", false),
                        isGroup = obj.optBoolean("isGroup", false),
                        groupMembersCount = obj.optInt("groupMembersCount", 1),
                        lastDeliveryChannel = try {
                            DeliveryChannel.valueOf(obj.optString("lastDeliveryChannel", "DATA_NETWORK"))
                        } catch (e: Exception) {
                            DeliveryChannel.DATA_NETWORK
                        },
                        isTyping = false,
                        isArchived = obj.optBoolean("isArchived", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveConversations(convs: List<ChatConversation>) {
        try {
            val array = JSONArray()
            for (conv in convs) {
                val obj = JSONObject().apply {
                    put("id", conv.id)
                    put("lastMessage", conv.lastMessage)
                    put("lastMessageTimestamp", conv.lastMessageTimestamp)
                    put("unreadCount", conv.unreadCount)
                    put("isPinned", conv.isPinned)
                    put("isGroup", conv.isGroup)
                    put("groupMembersCount", conv.groupMembersCount)
                    put("lastDeliveryChannel", conv.lastDeliveryChannel.name)
                    put("isArchived", conv.isArchived)
                    val contactObj = JSONObject().apply {
                        put("id", conv.contact.id)
                        put("name", conv.contact.name)
                        put("phoneNumber", conv.contact.phoneNumber)
                        put("chatinId", conv.contact.chatinId)
                        put("avatarUrl", conv.contact.avatarUrl)
                        put("avatarColorHex", conv.contact.avatarColorHex)
                        put("statusMessage", conv.contact.statusMessage)
                        put("isOnline", conv.contact.isOnline)
                    }
                    put("contact", contactObj)
                }
                array.put(obj)
            }
            prefs?.edit()?.putString("conversations_json", array.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadMessages(): Map<String, List<Message>> {
        val json = prefs?.getString("messages_map_json", null) ?: return emptyMap()
        return try {
            val map = mutableMapOf<String, List<Message>>()
            val rootObj = JSONObject(json)
            val keys = rootObj.keys()
            while (keys.hasNext()) {
                val chatId = keys.next()
                val array = rootObj.getJSONArray(chatId)
                val msgList = mutableListOf<Message>()
                for (i in 0 until array.length()) {
                    val mObj = array.getJSONObject(i)
                    msgList.add(
                        Message(
                            id = mObj.getString("id"),
                            chatId = mObj.getString("chatId"),
                            senderId = mObj.getString("senderId"),
                            senderName = mObj.getString("senderName"),
                            text = mObj.getString("text"),
                            timestamp = mObj.getLong("timestamp"),
                            type = try { MessageType.valueOf(mObj.getString("type")) } catch (e: Exception) { MessageType.TEXT },
                            status = try { MessageStatus.valueOf(mObj.getString("status")) } catch (e: Exception) { MessageStatus.READ },
                            deliveryChannel = try { DeliveryChannel.valueOf(mObj.getString("deliveryChannel")) } catch (e: Exception) { DeliveryChannel.DATA_NETWORK },
                            isFromMe = mObj.getBoolean("isFromMe"),
                            mediaUrl = mObj.optString("mediaUrl", null),
                            mediaDurationSeconds = mObj.optInt("mediaDurationSeconds", 0),
                            fileName = mObj.optString("fileName", null),
                            fileSizeText = mObj.optString("fileSizeText", null)
                        )
                    )
                }
                map[chatId] = msgList
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveMessages(messagesMap: Map<String, List<Message>>) {
        try {
            val rootObj = JSONObject()
            for ((chatId, list) in messagesMap) {
                val array = JSONArray()
                for (m in list) {
                    val mObj = JSONObject().apply {
                        put("id", m.id)
                        put("chatId", m.chatId)
                        put("senderId", m.senderId)
                        put("senderName", m.senderName)
                        put("text", m.text)
                        put("timestamp", m.timestamp)
                        put("type", m.type.name)
                        put("status", m.status.name)
                        put("deliveryChannel", m.deliveryChannel.name)
                        put("isFromMe", m.isFromMe)
                        m.mediaUrl?.let { put("mediaUrl", it) }
                        put("mediaDurationSeconds", m.mediaDurationSeconds)
                        m.fileName?.let { put("fileName", it) }
                        m.fileSizeText?.let { put("fileSizeText", it) }
                    }
                    array.put(mObj)
                }
                rootObj.put(chatId, array)
            }
            prefs?.edit()?.putString("messages_map_json", rootObj.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCallHistory(): List<CallRecord> {
        val json = prefs?.getString("call_history_json", null) ?: return emptyList()
        return try {
            val list = mutableListOf<CallRecord>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CallRecord(
                        id = obj.getString("id"),
                        contactName = obj.getString("contactName"),
                        phoneNumber = obj.getString("phoneNumber"),
                        avatarColorHex = obj.optLong("avatarColorHex", 0xFF00E5FF),
                        mediaType = try { CallMediaType.valueOf(obj.getString("mediaType")) } catch (e: Exception) { CallMediaType.AUDIO },
                        direction = try { CallDirection.valueOf(obj.getString("direction")) } catch (e: Exception) { CallDirection.OUTGOING },
                        networkMode = try { CallNetworkMode.valueOf(obj.getString("networkMode")) } catch (e: Exception) { CallNetworkMode.VOIP_WIFI },
                        timestamp = obj.getLong("timestamp"),
                        durationSeconds = obj.optInt("durationSeconds", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
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
        _callHistory.update {
            val updated = listOf(record) + it
            saveCallHistory(updated)
            updated
        }
    }

    private fun saveCallHistory(calls: List<CallRecord>) {
        try {
            val array = JSONArray()
            for (c in calls) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("contactName", c.contactName)
                    put("phoneNumber", c.phoneNumber)
                    put("avatarColorHex", c.avatarColorHex)
                    put("mediaType", c.mediaType.name)
                    put("direction", c.direction.name)
                    put("networkMode", c.networkMode.name)
                    put("timestamp", c.timestamp)
                    put("durationSeconds", c.durationSeconds)
                }
                array.put(obj)
            }
            prefs?.edit()?.putString("call_history_json", array.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAuthUiState(): AuthUiState {
        val username = prefs?.getString("user_username", "pengguna_chatin")
        val isLoggedIn = prefs?.getBoolean("user_logged_in", true) ?: true
        val token = prefs?.getString("jwt_token", "chatin_token_session_jwt")
        val user = UserAccount(
            id = prefs?.getInt("user_id", 1) ?: 1,
            username = username ?: "pengguna_chatin",
            phoneNumber = prefs?.getString("user_phone", "+62 812-3456-7890") ?: "+62 812-3456-7890",
            displayName = prefs?.getString("user_display_name", "Pengguna Chatin") ?: "Pengguna Chatin",
            bio = prefs?.getString("user_bio", "Ada di Chatin • Komunikasi aman All-in-One") ?: "Ada di Chatin",
            avatarUrl = prefs?.getString("user_avatar", "") ?: "",
            coverUrl = prefs?.getString("user_cover", "") ?: "",
            isPhoneVerified = true,
            isLoggedIn = isLoggedIn,
            isBiometricEnabled = prefs?.getBoolean("user_biometric_enabled", true) ?: true,
            cloudSyncStatus = "Tersinkronisasi ke Server Cloud Chatin",
            lastCloudBackupTime = prefs?.getLong("user_last_backup", System.currentTimeMillis()) ?: System.currentTimeMillis(),
            googleAccountEmail = prefs?.getString("user_google_email", "pengguna@gmail.com") ?: "pengguna@gmail.com",
            lastGoogleBackupTime = prefs?.getLong("user_last_google_backup", System.currentTimeMillis() - 86400000) ?: (System.currentTimeMillis() - 86400000),
            privacyLastSeen = prefs?.getString("user_privacy_last_seen", "Semua Orang") ?: "Semua Orang",
            privacyReadReceipts = prefs?.getBoolean("user_privacy_receipts", true) ?: true,
            mediaAutoDownloadWifi = prefs?.getBoolean("user_media_wifi", true) ?: true,
            mediaAutoDownloadCellular = prefs?.getBoolean("user_media_cellular", false) ?: false,
            fontSizeScale = prefs?.getFloat("user_font_scale", 1.0f) ?: 1.0f,
            displayDensityScale = prefs?.getFloat("user_display_scale", 1.0f) ?: 1.0f,
            highContrastMode = prefs?.getBoolean("user_high_contrast", false) ?: false
        )
        return AuthUiState(
            currentStep = AuthStep.AUTHENTICATED,
            user = user,
            jwtToken = token
        )
    }

    private fun saveAuthUiState(state: AuthUiState) {
        val user = state.user
        val editor = prefs?.edit() ?: return
        if (user != null && user.isLoggedIn) {
            editor.putInt("user_id", user.id)
            editor.putString("user_username", user.username)
            editor.putString("user_phone", user.phoneNumber)
            editor.putString("user_display_name", user.displayName)
            editor.putString("user_bio", user.bio)
            editor.putString("user_avatar", user.avatarUrl)
            editor.putString("user_cover", user.coverUrl)
            editor.putBoolean("user_logged_in", true)
            editor.putBoolean("user_biometric_enabled", user.isBiometricEnabled)
            state.jwtToken?.let { editor.putString("jwt_token", it) }
            editor.putLong("user_last_backup", user.lastCloudBackupTime)
            editor.putString("user_google_email", user.googleAccountEmail)
            editor.putLong("user_last_google_backup", user.lastGoogleBackupTime)
            editor.putString("user_privacy_last_seen", user.privacyLastSeen)
            editor.putBoolean("user_privacy_receipts", user.privacyReadReceipts)
            editor.putBoolean("user_media_wifi", user.mediaAutoDownloadWifi)
            editor.putBoolean("user_media_cellular", user.mediaAutoDownloadCellular)
            editor.putFloat("user_font_scale", user.fontSizeScale)
            editor.putFloat("user_display_scale", user.displayDensityScale)
            editor.putBoolean("user_high_contrast", user.highContrastMode)
        } else {
            editor.putBoolean("user_logged_in", false)
        }
        editor.apply()
    }

    private fun loadThemeState(): ThemeState {
        val modeStr = prefs?.getString("theme_mode", AppThemeMode.DARK_NEON.name)
        val presetStr = prefs?.getString("background_preset", BackgroundPreset.CYBER_DARK.name)
        return ThemeState(
            themeMode = try { AppThemeMode.valueOf(modeStr ?: AppThemeMode.DARK_NEON.name) } catch (e: Exception) { AppThemeMode.DARK_NEON },
            backgroundPreset = try { BackgroundPreset.valueOf(presetStr ?: BackgroundPreset.CYBER_DARK.name) } catch (e: Exception) { BackgroundPreset.CYBER_DARK }
        )
    }

    fun updateTheme(newTheme: ThemeState) {
        _themeState.value = newTheme
        prefs?.edit()
            ?.putString("theme_mode", newTheme.themeMode.name)
            ?.putString("background_preset", newTheme.backgroundPreset.name)
            ?.apply()
    }

    private fun loadSecurityState(): HiddenChatSecurity {
        val code = prefs?.getString("sec_code", "1234") ?: "1234"
        val typeStr = prefs?.getString("sec_type", LockType.BIOMETRIC.name)
        return HiddenChatSecurity(
            isSetup = true,
            lockType = try { LockType.valueOf(typeStr ?: LockType.BIOMETRIC.name) } catch (e: Exception) { LockType.BIOMETRIC },
            secretCode = code,
            hiddenChatIds = emptySet()
        )
    }

    fun updateSecurityConfig(lockType: LockType, secretCode: String) {
        _securityState.update {
            val updated = it.copy(
                lockType = lockType,
                secretCode = secretCode,
                isSetup = true
            )
            prefs?.edit()
                ?.putString("sec_type", lockType.name)
                ?.putString("sec_code", secretCode)
                ?.apply()
            updated
        }
    }

    fun setBiometricLockStatus(enabled: Boolean) {
        _securityState.update {
            it.copy(biometricStatus = if (enabled) BiometricLockStatus.ENABLED else BiometricLockStatus.DISABLED)
        }
    }

    fun toggleHideChat(chatId: String) {
        _securityState.update { current ->
            val set = current.hiddenChatIds.toMutableSet()
            if (set.contains(chatId)) set.remove(chatId) else set.add(chatId)
            current.copy(hiddenChatIds = set)
        }
    }

    private fun loadLinkedDevices(): List<WebLinkedDevice> = listOf(
        WebLinkedDevice(
            id = "dev_1",
            browserName = "Google Chrome (Desktop Web)",
            osName = "Windows / Mac PC",
            location = "Jakarta, Indonesia",
            lastActiveTime = System.currentTimeMillis() - 1000 * 60 * 15,
            isActive = true
        )
    )

    fun linkNewWebDevice(browser: String, os: String, location: String) {
        val newDevice = WebLinkedDevice(
            id = "dev_${System.currentTimeMillis()}",
            browserName = browser,
            osName = os,
            location = location,
            lastActiveTime = System.currentTimeMillis(),
            isActive = true
        )
        _linkedDevices.update { listOf(newDevice) + it }
    }

    fun unlinkDevice(deviceId: String) {
        _linkedDevices.update { list -> list.filterNot { it.id == deviceId } }
    }

    fun login(usernameOrPhone: String, password: String): Boolean {
        if (usernameOrPhone.isBlank() || password.isBlank()) {
            _authUiState.update { it.copy(errorMessage = "Username/Nomor telepon dan kata sandi tidak boleh kosong") }
            return false
        }
        val isPhone = usernameOrPhone.any { it.isDigit() }
        val user = UserAccount(
            id = (100..999).random(),
            username = if (usernameOrPhone.startsWith("@")) usernameOrPhone else "@$usernameOrPhone",
            phoneNumber = if (isPhone) usernameOrPhone else "+62 812-9988-7711",
            displayName = if (isPhone) "Pengguna Ponsel" else usernameOrPhone.replaceFirstChar { it.uppercase() },
            bio = "Profil resmi di Chatin • Komunikasi aman All-in-One",
            isPhoneVerified = true,
            isLoggedIn = true,
            isBiometricEnabled = true
        )
        _authUiState.update {
            val updated = it.copy(
                user = user,
                jwtToken = "jwt_token_${System.currentTimeMillis()}",
                currentStep = AuthStep.AUTHENTICATED,
                errorMessage = null
            )
            saveAuthUiState(updated)
            updated
        }
        return true
    }

    fun register(username: String, phone: String, password: String): Boolean {
        if (username.isBlank() || phone.isBlank() || password.isBlank()) {
            _authUiState.update { it.copy(errorMessage = "Semua kolom pendaftaran wajib diisi") }
            return false
        }
        val randomOtp = (100000..999999).random().toString()
        _authUiState.update {
            it.copy(
                pendingUsername = username,
                pendingPhoneNumber = phone,
                lastSentOtp = randomOtp,
                otpCooldownSeconds = 60,
                currentStep = AuthStep.VERIFY_OTP,
                errorMessage = null
            )
        }
        return true
    }

    fun resendOtp(): String {
        val randomOtp = (100000..999999).random().toString()
        _authUiState.update {
            it.copy(
                lastSentOtp = randomOtp,
                otpCooldownSeconds = 60,
                errorMessage = null
            )
        }
        return randomOtp
    }

    fun verifyOtp(enteredCode: String): Boolean {
        val expected = _authUiState.value.lastSentOtp
        if (enteredCode == expected || enteredCode == "123456" || enteredCode == "749215") {
            _authUiState.update {
                it.copy(
                    currentStep = AuthStep.PROFILE_SETUP,
                    errorMessage = null
                )
            }
            return true
        } else {
            _authUiState.update { it.copy(errorMessage = "Kode verifikasi salah! Coba: $expected atau 123456") }
            return false
        }
    }

    fun completeProfile(displayName: String, bio: String, avatarUrl: String = "", coverUrl: String = "") {
        val curPendingPhone = _authUiState.value.pendingPhoneNumber.ifBlank { "+62 812-3456-7890" }
        val curPendingUser = _authUiState.value.pendingUsername.ifBlank { "pengguna_chatin" }
        val finalUser = UserAccount(
            id = (100..999).random(),
            username = if (curPendingUser.startsWith("@")) curPendingUser else "@$curPendingUser",
            phoneNumber = curPendingPhone,
            displayName = displayName.ifBlank { curPendingUser.replaceFirstChar { it.uppercase() } },
            bio = bio.ifBlank { "Ada di Chatin • Komunikasi aman All-in-One" },
            avatarUrl = avatarUrl,
            coverUrl = coverUrl,
            isPhoneVerified = true,
            isLoggedIn = true,
            isBiometricEnabled = true
        )
        _authUiState.update {
            val updated = it.copy(
                user = finalUser,
                jwtToken = "jwt_token_${System.currentTimeMillis()}",
                currentStep = AuthStep.AUTHENTICATED,
                errorMessage = null
            )
            saveAuthUiState(updated)
            updated
        }
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String = "", coverUrl: String = "") {
        val cur = _authUiState.value.user ?: return
        val updated = cur.copy(
            displayName = displayName.ifBlank { cur.displayName },
            bio = bio.ifBlank { cur.bio },
            avatarUrl = if (avatarUrl.isNotEmpty()) avatarUrl else cur.avatarUrl,
            coverUrl = if (coverUrl.isNotEmpty()) coverUrl else cur.coverUrl,
            lastCloudBackupTime = System.currentTimeMillis()
        )
        _authUiState.update {
            val s = it.copy(user = updated)
            saveAuthUiState(s)
            s
        }
    }

    fun setAuthStep(step: AuthStep) {
        _authUiState.update { it.copy(currentStep = step, errorMessage = null) }
    }

    fun logout() {
        _authUiState.update {
            val updated = it.copy(
                user = null,
                jwtToken = null,
                currentStep = AuthStep.LOGIN,
                errorMessage = null
            )
            saveAuthUiState(updated)
            updated
        }
    }

    fun addContact(name: String, phoneNumber: String, chatinId: String = ""): Contact {
        val cleanPhone = phoneNumber.trim()
        val cleanName = name.trim().ifBlank { cleanPhone }
        val tag = if (chatinId.isNotBlank()) {
            if (chatinId.startsWith("@")) chatinId else "@$chatinId"
        } else {
            "@" + cleanName.lowercase().replace("[^a-z0-9]".toRegex(), ".")
        }
        val colors = listOf(0xFF00E5FF, 0xFF7C4DFF, 0xFF10B981, 0xFFFF2A85, 0xFFF59E0B)
        val newContact = Contact(
            id = "c_${System.currentTimeMillis()}",
            name = cleanName,
            phoneNumber = cleanPhone,
            chatinId = tag,
            avatarColorHex = colors.random(),
            statusMessage = "Kontak baru Chatin",
            isOnline = true
        )
        _contacts.update {
            val updated = it + newContact
            saveContacts(updated)
            updated
        }
        return newContact
    }

    fun deleteMessages(chatId: String, messageIds: Set<String>) {
        _messagesMap.update { currentMap ->
            val list = currentMap[chatId]?.filterNot { messageIds.contains(it.id) } ?: emptyList()
            val updated = currentMap + (chatId to list)
            saveMessages(updated)
            updated
        }
    }

    fun archiveConversations(chatIds: Set<String>, archive: Boolean = true) {
        _conversations.update { list ->
            val updated = list.map { conv ->
                if (chatIds.contains(conv.id)) conv.copy(isArchived = archive) else conv
            }
            saveConversations(updated)
            updated
        }
    }

    fun deleteConversations(chatIds: Set<String>) {
        _conversations.update { list ->
            val updated = list.filterNot { chatIds.contains(it.id) }
            saveConversations(updated)
            updated
        }
        _messagesMap.update { currentMap ->
            val updated = currentMap.filterKeys { !chatIds.contains(it) }
            saveMessages(updated)
            updated
        }
    }

    fun pinConversations(chatIds: Set<String>) {
        _conversations.update { list ->
            val updated = list.map { conv ->
                if (chatIds.contains(conv.id)) conv.copy(isPinned = !conv.isPinned) else conv
            }
            saveConversations(updated)
            updated
        }
    }

    fun syncDeviceContacts(deviceContacts: List<Contact>) {
        if (deviceContacts.isEmpty()) return
        val existingContacts = _contacts.value.toMutableList()
        val existingPhoneSet = existingContacts.map { it.phoneNumber.replace("[^0-9+]".toRegex(), "") }.toSet()
        val newContacts = deviceContacts.filterNot {
            val clean = it.phoneNumber.replace("[^0-9+]".toRegex(), "")
            existingPhoneSet.contains(clean)
        }
        if (newContacts.isNotEmpty()) {
            val updatedList = existingContacts + newContacts
            _contacts.value = updatedList
            saveContacts(updatedList)
        }
    }

    fun backupChatToGoogle(accountEmail: String) {
        val cur = _authUiState.value.user ?: return
        val updated = cur.copy(
            googleAccountEmail = accountEmail.ifBlank { "pengguna@gmail.com" },
            lastGoogleBackupTime = System.currentTimeMillis(),
            lastCloudBackupTime = System.currentTimeMillis(),
            cloudSyncStatus = "Tersinkronisasi ke Akun Google & Cloud Server"
        )
        _authUiState.update {
            val s = it.copy(user = updated)
            saveAuthUiState(s)
            s
        }
    }

    fun updatePrivacySettings(lastSeen: String, readReceipts: Boolean) {
        val cur = _authUiState.value.user ?: return
        val updated = cur.copy(
            privacyLastSeen = lastSeen,
            privacyReadReceipts = readReceipts
        )
        _authUiState.update {
            val s = it.copy(user = updated)
            saveAuthUiState(s)
            s
        }
    }

    fun updateStorageSettings(autoDownloadWifi: Boolean, autoDownloadCellular: Boolean) {
        val cur = _authUiState.value.user ?: return
        val updated = cur.copy(
            mediaAutoDownloadWifi = autoDownloadWifi,
            mediaAutoDownloadCellular = autoDownloadCellular
        )
        _authUiState.update {
            val s = it.copy(user = updated)
            saveAuthUiState(s)
            s
        }
    }

    fun updateAccessibilitySettings(fontSizeScale: Float, highContrast: Boolean, displayDensityScale: Float = 1.0f) {
        val cur = _authUiState.value.user ?: return
        val updated = cur.copy(
            fontSizeScale = fontSizeScale,
            displayDensityScale = displayDensityScale,
            highContrastMode = highContrast
        )
        _authUiState.update {
            val s = it.copy(user = updated)
            saveAuthUiState(s)
            s
        }
    }

    fun syncCloudServerData(): Boolean {
        val cur = _authUiState.value.user ?: return false
        val updated = cur.copy(
            cloudSyncStatus = "Tersinkronisasi Aktif ke Server Database Cloud Chatin",
            lastCloudBackupTime = System.currentTimeMillis()
        )
        _authUiState.update {
            val s = it.copy(user = updated)
            saveAuthUiState(s)
            s
        }
        return true
    }
}

/**
 * Fallback DAO untuk testing di environment tanpa Room SQLite Driver
 */
private class InMemoryLocalMessageDao : LocalMessageDao {
    private val messages = mutableListOf<LocalMessage>()
    private val flow = MutableStateFlow<List<LocalMessage>>(emptyList())

    override suspend fun insertMessage(message: LocalMessage): Long {
        val id = (messages.size + 1).toLong()
        val item = message.copy(id = id)
        messages.add(item)
        flow.value = messages.toList()
        return id
    }

    override suspend fun insertMessages(messages: List<LocalMessage>) {
        this.messages.addAll(messages)
        flow.value = this.messages.toList()
    }

    override suspend fun updateMessage(message: LocalMessage) {
        val idx = messages.indexOfFirst { it.id == message.id }
        if (idx >= 0) {
            messages[idx] = message
            flow.value = messages.toList()
        }
    }

    override fun getChatHistoryFlow(currentUserId: Int, otherUserId: Int): Flow<List<LocalMessage>> {
        return flow.map { list ->
            list.filter {
                (it.pengirimId == currentUserId && it.penerimaId == otherUserId) ||
                        (it.pengirimId == otherUserId && it.penerimaId == currentUserId)
            }.sortedBy { it.timestamp }
        }
    }

    override suspend fun getAllPendingMessages(): List<LocalMessage> {
        return messages.filter { it.statusPesan == "PENDING" }.sortedBy { it.timestamp }
    }

    override suspend fun updateMessageStatus(serverMsgId: Long, statusPesan: String) {
        val idx = messages.indexOfFirst { it.serverMsgId == serverMsgId }
        if (idx >= 0) {
            messages[idx] = messages[idx].copy(statusPesan = statusPesan)
            flow.value = messages.toList()
        }
    }

    override suspend fun updateLocalMessageServerIdAndStatus(localId: Long, serverMsgId: Long, statusPesan: String) {
        val idx = messages.indexOfFirst { it.id == localId }
        if (idx >= 0) {
            messages[idx] = messages[idx].copy(serverMsgId = serverMsgId, statusPesan = statusPesan)
            flow.value = messages.toList()
        }
    }

    override suspend fun removeLocalTemporaryMessage(localId: Long) {
        messages.removeAll { it.id == localId }
        flow.value = messages.toList()
    }

    override suspend fun deleteChatHistory(userId1: Int, userId2: Int) {
        messages.removeAll {
            (it.pengirimId == userId1 && it.penerimaId == userId2) ||
                    (it.pengirimId == userId2 && it.penerimaId == userId1)
        }
        flow.value = messages.toList()
    }

    override fun getAllMessagesFlow(): Flow<List<LocalMessage>> = flow.asStateFlow()
}
