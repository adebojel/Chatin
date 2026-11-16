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
 * Status acknowledgement pesan. DELIVERED/READ hanya boleh dipakai jika ada receipt server/perangkat yang terverifikasi.
 */
enum class MessageStatus {
    PENDING,     // Ikon jam pasir (dalam antrian offline lokal)
    SENT,        // Centang 1 abu-abu (berhasil mencapai server awan)
    DELIVERED,   // Centang 2 abu-abu (tersampaikan ke perangkat penerima)
    READ         // Centang 2 biru (dibaca oleh pengguna target)
}

/**
 * Model label saluran pesan. RCS dan SMS fallback belum diimplementasikan oleh transport Chatin.
 */
enum class DeliveryChannel {
    DATA_NETWORK, // Internet standar (Wi-Fi HD / Data Seluler 5G)
    RCS_ADVANCED, // Belum tersedia: belum ada integrasi RCS carrier
    SMS_FALLBACK; // Belum tersedia: belum ada integrasi SMS operator

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
    val statusMessage: String = "",
    val isOnline: Boolean = false,
    val lastSeenText: String = "Status tidak diketahui",
    val isFavorite: Boolean = false,
    val hasRcs: Boolean = false,
    val allowsVoip: Boolean = false
)

/**
 * Model pesan dalam memori klien. Field model ini tidak menyatakan payload terenkripsi end-to-end.
 */
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.PENDING,
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
