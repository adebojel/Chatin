package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entitas Room Database untuk Replikasi Cache Pesan Offline Lokal Chatin.
 * Dilengkapi dengan indeks performa tinggi untuk pencarian percakapan cepat dan query sekuensial FIFO.
 */
@Entity(
    tableName = "local_messages",
    indices = [
        Index(value = ["pengirimId", "penerimaId"], name = "idx_local_chat_flow"),
        Index(value = ["serverMsgId"], name = "idx_local_server_msg_id"),
        Index(value = ["statusPesan"], name = "idx_local_status_pesan"),
        Index(value = ["timestamp"], name = "idx_local_timestamp")
    ]
)
data class LocalMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // ID server resmi yang diberikan oleh backend PostgreSQL (0 atau stempel waktu sementara jika dibuat saat offline)
    val serverMsgId: Long = 0L,

    val pengirimId: Int,
    val penerimaId: Int,

    // Tipe pesan: text, audio, image, video, document, location, call_event
    val msgType: String = "text",

    // Teks pesan (baik terenkripsi maupun plaintext terdekripsi lokal di sandbox)
    val textText: String,

    // URL media CDN di S3/Cloudflare R2 (jika ada lampiran)
    val cloudMediaUrl: String? = null,

    // Pratinjau mikro 16x16 piksel buram yang dienkripsi AES-GCM
    val mediaThumbnail: String? = null,

    // Counter ratcheting Signal Protocol untuk mencegah replay attacks
    val counter: Int = 0,

    // Status siklus hidup pesan: PENDING, sent, delivered, read
    val statusPesan: String = "PENDING",

    // Stempel waktu pembuatan lokal (milidetik)
    val timestamp: Long = System.currentTimeMillis()
)
