package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) Room Database untuk operasi pesan offline Chatin.
 * Mendukung aliran data realtime Flow<List<LocalMessage>> dan antrian FIFO berurutan kronologis.
 */
@Dao
interface LocalMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<LocalMessage>)

    @Update
    suspend fun updateMessage(message: LocalMessage)

    // Menarik riwayat obrolan dua arah antara pengirim dan penerima terurut kronologis secara reaktif
    @Query("""
        SELECT * FROM local_messages 
        WHERE (pengirimId = :currentUserId AND penerimaId = :otherUserId)
           OR (pengirimId = :otherUserId AND penerimaId = :currentUserId)
        ORDER BY timestamp ASC
    """)
    fun getChatHistoryFlow(currentUserId: Int, otherUserId: Int): Flow<List<LocalMessage>>

    // Menarik semua antrian pesan tertunda yang dibuat saat mode pesawat/offline dengan metode FIFO (timestamp ASC)
    @Query("SELECT * FROM local_messages WHERE statusPesan = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getAllPendingMessages(): List<LocalMessage>

    // Memperbarui status pengiriman pesan berdasarkan ID server resmi
    @Query("UPDATE local_messages SET statusPesan = :statusPesan WHERE serverMsgId = :serverMsgId")
    suspend fun updateMessageStatus(serverMsgId: Long, statusPesan: String)

    // Memperbarui ID server resmi dan status pesan setelah sinkronisasi berhasil
    @Query("UPDATE local_messages SET serverMsgId = :serverMsgId, statusPesan = :statusPesan WHERE id = :localId")
    suspend fun updateLocalMessageServerIdAndStatus(localId: Long, serverMsgId: Long, statusPesan: String)

    // Menghapus ID pesan sementara lokal setelah berhasil digantikan oleh ID server resmi
    @Query("DELETE FROM local_messages WHERE id = :localId")
    suspend fun removeLocalTemporaryMessage(localId: Long)

    // Menghapus seluruh riwayat percakapan antara dua pengguna
    @Query("""
        DELETE FROM local_messages 
        WHERE (pengirimId = :userId1 AND penerimaId = :userId2)
           OR (pengirimId = :userId2 AND penerimaId = :userId1)
    """)
    suspend fun deleteChatHistory(userId1: Int, userId2: Int)

    // Mengambil semua pesan di database untuk kebutuhan sinkronisasi global
    @Query("SELECT * FROM local_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<LocalMessage>>
}
