package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalMessage
import com.example.data.models.CallMediaType
import com.example.data.models.CallNetworkMode
import com.example.data.models.DeliveryChannel
import com.example.data.models.MessageType
import com.example.data.repository.ChatinRepository
import com.example.util.CryptoHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies chatin app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("chatin", appName)
    }

    @Test
    fun `crypto helper ECDH key agreement and AES-GCM 256 encryption roundtrip`() {
        // Generate key pairs for Alice and Bob
        val aliceKeyPair = CryptoHelper.generateEcKeyPair()
        val bobKeyPair = CryptoHelper.generateEcKeyPair()

        val alicePublicKeyBase64 = android.util.Base64.encodeToString(aliceKeyPair.public.encoded, android.util.Base64.NO_WRAP)
        val bobPublicKeyBase64 = android.util.Base64.encodeToString(bobKeyPair.public.encoded, android.util.Base64.NO_WRAP)

        // Compute shared secret independently
        val aliceSharedSecret = CryptoHelper.computeSharedSecretKey(aliceKeyPair.private, bobPublicKeyBase64)
        val bobSharedSecret = CryptoHelper.computeSharedSecretKey(bobKeyPair.private, alicePublicKeyBase64)

        // Verify identical 256-bit symmetric keys
        assertArrayEquals(aliceSharedSecret.encoded, bobSharedSecret.encoded)

        // Encrypt with Alice's key, decrypt with Bob's key
        val originalPlaintext = "Pesan rahasia E2EE Chatin dengan enkripsi mutlak"
        val ciphertext = CryptoHelper.encryptAesGcm(originalPlaintext, aliceSharedSecret)
        assertNotEquals(originalPlaintext, ciphertext)

        val decryptedPlaintext = CryptoHelper.decryptAesGcm(ciphertext, bobSharedSecret)
        assertEquals(originalPlaintext, decryptedPlaintext)
    }

    @Test
    fun `repository sends message with dual network channels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ChatinRepository(context)
        val conv = repo.startOrGetChat("08123456789")
        val chatId = conv.id
        val initialCount = repo.getMessagesForChat(chatId).size

        repo.sendMessage(
            chatId = chatId,
            text = "Testing fallback SMS network",
            type = MessageType.TEXT,
            channel = DeliveryChannel.SMS_FALLBACK
        )

        val updatedMessages = repo.getMessagesForChat(chatId)
        assertEquals(initialCount + 1, updatedMessages.size)
        val lastMsg = updatedMessages.last()
        assertEquals("Testing fallback SMS network", lastMsg.text)
        assertEquals(DeliveryChannel.SMS_FALLBACK, lastMsg.deliveryChannel)
        assertTrue(lastMsg.isFromMe)
    }

    @Test
    fun `offline message caching and FIFO queue verification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ChatinRepository(context)

        val pendingMsg1 = LocalMessage(
            serverMsgId = 1001L,
            pengirimId = 1,
            penerimaId = 2,
            textText = "Pesan Antrian 1",
            statusPesan = "PENDING",
            timestamp = 1000000L
        )
        val pendingMsg2 = LocalMessage(
            serverMsgId = 1002L,
            pengirimId = 1,
            penerimaId = 2,
            textText = "Pesan Antrian 2",
            statusPesan = "PENDING",
            timestamp = 2000000L
        )

        repo.cacheIncomingMessageLocally(pendingMsg1)
        repo.cacheIncomingMessageLocally(pendingMsg2)

        val pendingQueue = repo.localMessageDao.getAllPendingMessages()
        assertTrue(pendingQueue.size >= 2)
        // Verify FIFO order (timestamp ASC)
        val timestamps = pendingQueue.map { it.timestamp }
        assertEquals(timestamps.sorted(), timestamps)
    }

    @Test
    fun `web linked devices pairing and unlinking`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ChatinRepository(context)
        val initialCount = repo.linkedDevices.value.size
        repo.linkNewWebDevice("Firefox Web Client", "Ubuntu Linux", "Bandung, Indonesia")
        val updatedDevices = repo.linkedDevices.value
        assertEquals(initialCount + 1, updatedDevices.size)
        assertEquals("Firefox Web Client", updatedDevices.first().browserName)
        val newDeviceId = updatedDevices.first().id
        repo.unlinkDevice(newDeviceId)
        assertEquals(initialCount, repo.linkedDevices.value.size)
    }
}
