package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.data.models.ChatConversation
import com.example.data.models.Contact
import com.example.data.models.DeliveryChannel
import com.example.data.models.Message
import com.example.data.models.MessageStatus
import com.example.data.models.MessageType
import com.example.data.models.UserAccount
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

/**
 * Enterprise Firebase Manager untuk Chatin.
 * Mengintegrasikan Firebase Authentication (SMS OTP asli dengan PhoneAuthProvider)
 * dan Firebase Cloud Firestore untuk sinkronisasi pesan chat real-time tanpa refresh.
 */
object FirebaseManager {

    private const val TAG = "FirebaseManager"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    // Fallback inisialisasi default jika google-services.json belum diinjeksikan secara statis
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:385344590708:android:chatinapp")
                        .setProjectId("chatin-official")
                        .setApiKey("AIzaSyChatinOfficialMobileKey2026")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
            }
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}")
        }
    }

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not ready: ${e.message}")
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore not ready: ${e.message}")
            null
        }

    // =========================================================================
    // 1. FIREBASE AUTHENTICATION (SMS OTP ASLI NOMOR TELEPON)
    // =========================================================================

    fun sendPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationCompleted: (credential: PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val authInstance = auth ?: run {
            onVerificationFailed(IllegalStateException("Firebase Auth tidak tersedia"))
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Phone verification automatically completed.")
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                Log.e(TAG, "Phone verification failed: ${e.message}")
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "SMS OTP verification code sent to $phoneNumber.")
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(authInstance)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun signInWithOtpCode(
        verificationId: String,
        smsCode: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val authInstance = auth ?: run {
            onError(IllegalStateException("Firebase Auth tidak aktif"))
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        authInstance.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) } ?: onError(IllegalStateException("Pengguna Firebase kosong"))
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // =========================================================================
    // 2. FIRESTORE REAL-TIME CHAT & PESAN (TANPA REFRESH)
    // =========================================================================

    /**
     * Mengamati aliran pesan real-time dari Firestore untuk chatId tertentu.
     */
    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing Firestore messages: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            Message(
                                id = doc.getString("id") ?: doc.id,
                                chatId = chatId,
                                senderId = doc.getString("senderId") ?: "",
                                senderName = doc.getString("senderName") ?: "Pengirim",
                                text = doc.getString("text") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                type = try {
                                    MessageType.valueOf(doc.getString("type") ?: MessageType.TEXT.name)
                                } catch (e: Exception) {
                                    MessageType.TEXT
                                },
                                status = try {
                                    MessageStatus.valueOf(doc.getString("status") ?: MessageStatus.READ.name)
                                } catch (e: Exception) {
                                    MessageStatus.READ
                                },
                                deliveryChannel = try {
                                    DeliveryChannel.valueOf(doc.getString("channel") ?: DeliveryChannel.DATA_NETWORK.name)
                                } catch (e: Exception) {
                                    DeliveryChannel.DATA_NETWORK
                                },
                                isFromMe = doc.getBoolean("isFromMe") ?: false,
                                fileName = doc.getString("fileName"),
                                fileSizeText = doc.getString("fileSizeText"),
                                mediaUrl = doc.getString("mediaUrl"),
                                mediaDurationSeconds = doc.getLong("mediaDurationSeconds")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(messages)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Mengirim pesan ke database Cloud Firestore real-time.
     */
    fun sendMessage(
        chatId: String,
        message: Message,
        contactName: String,
        contactPhone: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val db = firestore ?: return
        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document(message.id)

        val msgData = hashMapOf(
            "id" to message.id,
            "chatId" to chatId,
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "text" to message.text,
            "timestamp" to message.timestamp,
            "isFromMe" to message.isFromMe,
            "status" to message.status.name,
            "type" to message.type.name,
            "channel" to message.deliveryChannel.name,
            "fileName" to message.fileName,
            "fileSizeText" to message.fileSizeText,
            "mediaUrl" to message.mediaUrl,
            "mediaDurationSeconds" to message.mediaDurationSeconds
        )

        val chatMetadata = hashMapOf(
            "id" to chatId,
            "lastMessage" to message.text,
            "lastMessageTimestamp" to message.timestamp,
            "lastDeliveryChannel" to message.deliveryChannel.name,
            "contactName" to contactName,
            "contactPhone" to contactPhone,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.runBatch { batch ->
            batch.set(msgRef, msgData)
            batch.set(chatRef, chatMetadata, SetOptions.merge())
        }.addOnSuccessListener {
            Log.d(TAG, "Message sent to Firestore successfully.")
            onSuccess()
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to send message to Firestore: ${e.message}")
            onError(e)
        }
    }

    /**
     * Memperbarui status kehadiran online pengguna di Firestore secara nyata.
     */
    fun updateUserPresence(userId: String, isOnline: Boolean) {
        val db = firestore ?: return
        if (userId.isBlank()) return
        val userRef = db.collection("users").document(userId)
        val updates = hashMapOf<String, Any>(
            "isOnline" to isOnline,
            "lastSeenTimestamp" to System.currentTimeMillis()
        )
        userRef.set(updates, SetOptions.merge()).addOnFailureListener {
            Log.w(TAG, "Presence update failed: ${it.message}")
        }
    }

    /**
     * Menyimpan profil pengguna resmi ke Firestore.
     */
    fun saveUserProfile(user: UserAccount) {
        val db = firestore ?: return
        val userRef = db.collection("users").document(user.phoneNumber)
        val data = hashMapOf(
            "id" to user.id,
            "username" to user.username,
            "phoneNumber" to user.phoneNumber,
            "displayName" to user.displayName,
            "bio" to user.bio,
            "avatarUrl" to user.avatarUrl,
            "isOnline" to true,
            "lastUpdated" to System.currentTimeMillis()
        )
        userRef.set(data, SetOptions.merge())
    }
}
