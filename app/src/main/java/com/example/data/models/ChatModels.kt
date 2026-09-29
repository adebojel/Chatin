package com.example.data.models

/**
 * Tipe pesan komunikasi Chatin
 */
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    LOCATION,
    CALL_EVENT
}

/**
 * Status pesan untuk Read Receipt (Sesuai protokol WhatsApp/Signal)
 */
enum class MessageStatus {
    PENDING,     // Ikon jam pasir (dalam antrian offline lokal)
    SENT,        // Centang 1 abu-abu (berhasil mencapai server awan)
    DELIVERED,   // Centang 2 abu-abu (tersampaikan ke perangkat penerima)
    READ         // Centang 2 biru (dibaca oleh pengguna target)
}

/**
 * Saluran pengiriman pesan multi-jaringan
 */
enum class DeliveryChannel {
    DATA_NETWORK, // Internet standar (Wi-Fi HD / Data Seluler 5G)
    RCS_ADVANCED, // Rich Communication Services carrier
    SMS_FALLBACK; // SMS operator GSM (Bebas kuota internet 0%)

    companion object {
        val GSM_SMS = SMS_FALLBACK
        val RCS_ENCRYPTED = RCS_ADVANCED
    }
}

/**
 * Model Kontak Buku Telepon / Direktori Chatin
 */
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val chatinId: String,
    val avatarUrl: String = "",
    val avatarColorHex: Long = 0xFF00E5FF,
    val statusMessage: String = "Menggunakan Chatin untuk komunikasi terenkripsi",
    val isOnline: Boolean = true,
    val lastSeenText: String = "Online",
    val isFavorite: Boolean = false,
    val hasRcs: Boolean = true,
    val allowsVoip: Boolean = true
)

/**
 * Model Pesan E2EE dalam Memori Klien
 */
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.READ,
    val deliveryChannel: DeliveryChannel = DeliveryChannel.DATA_NETWORK,
    val isFromMe: Boolean = false,
    val mediaUrl: String? = null,
    val mediaThumbnail: String? = null,
    val mediaDurationSeconds: Int = 0,
    val fileName: String? = null,
    val fileSizeText: String? = null,
    val locationLatitude: Double = 0.0,
    val locationLongitude: Double = 0.0,
    val reactionEmoji: String? = null
)

/**
 * Model Percakapan Obrolan Beranda
 */
data class ChatConversation(
    val id: String,
    val contact: Contact,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isGroup: Boolean = false,
    val groupMembersCount: Int = 1,
    val lastDeliveryChannel: DeliveryChannel = DeliveryChannel.DATA_NETWORK,
    val isTyping: Boolean = false,
    val isArchived: Boolean = false
)
