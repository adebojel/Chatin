package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise Cryptographic Engine for Chatin.
 * Mengimplementasikan Hybrid Cryptography (Signal Protocol Style):
 * 1. Asymmetric ECDH (Curve NIST P-256) untuk kesepakatan kunci rahasia bersama (Shared Secret)
 * 2. Symmetric AES-GCM 256-bit dengan 96-bit random IV & 128-bit Authentication Tag untuk Zero-Knowledge
 * 3. Pembuatan thumbnail mikro 16x16 piksel buram yang dienkripsi penuh di sandbox lokal.
 */
object CryptoHelper {

    private const val EC_CURVE = "secp256r1" // NIST P-256
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12 // 96 bits
    private const val GCM_TAG_LENGTH = 128 // 128 bits auth tag

    private val secureRandom = SecureRandom()

    /**
     * Menghasilkan pasangan kunci asimetris ECDH P-256 baru di sandbox perangkat klien
     */
    fun generateEcKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec(EC_CURVE)
        keyPairGenerator.initialize(ecSpec, secureRandom)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Menghitung kunci simetris rahasia bersama (Shared Secret) melalui Diffie-Hellman P-256
     * dan menerapkan SHA-256 KDF (Key Derivation Function) untuk menghasilkan kunci AES 256-bit.
     */
    fun computeSharedSecretKey(myPrivateKey: PrivateKey, peerPublicKeyBase64: String): SecretKeySpec {
        val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = KeyFactory.getInstance("EC")
        val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerKeyBytes))

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)

        val rawSharedSecret = keyAgreement.generateSecret()
        // Turunkan menjadi kunci AES 256-bit menggunakan SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = digest.digest(rawSharedSecret)

        return SecretKeySpec(aesKeyBytes, "AES")
    }

    /**
     * Enkripsi teks plaintext menggunakan AES-GCM 256-bit.
     * Output format: Base64(IV + Ciphertext + AuthTag)
     */
    fun encryptAesGcm(plaintext: String, secretKey: SecretKeySpec): String {
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherBytes = cipher.doFinal(plaintextBytes)

        val byteBuffer = ByteBuffer.allocate(iv.size + cipherBytes.size)
        byteBuffer.put(iv)
        byteBuffer.put(cipherBytes)

        return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
    }

    /**
     * Dekripsi ciphertext terenkripsi AES-GCM 256-bit di sandbox lokal.
     */
    fun decryptAesGcm(encryptedBase64Payload: String, secretKey: SecretKeySpec): String {
        val fullCipherBytes = Base64.decode(encryptedBase64Payload, Base64.NO_WRAP)
        if (fullCipherBytes.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Ciphertext payload terlalu pendek")
        }

        val byteBuffer = ByteBuffer.wrap(fullCipherBytes)
        val iv = ByteArray(GCM_IV_LENGTH)
        byteBuffer.get(iv)

        val cipherBytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(cipherBytes)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Membuat thumbnail mikro resolusi 16x16 piksel dari gambar asli,
     * mengompresnya dengan kualitas rendah, dan mengenkripsi hasilnya dengan AES-GCM.
     */
    fun createEncryptedMicroThumbnail(originalBitmap: Bitmap, secretKey: SecretKeySpec): String {
        // Skala ke mikro resolusi 16x16 piksel
        val microBitmap = Bitmap.createScaledBitmap(originalBitmap, 16, 16, true)
        val outputStream = ByteArrayOutputStream()
        microBitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
        val compressedBytes = outputStream.toByteArray()

        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encryptedBytes = cipher.doFinal(compressedBytes)

        val byteBuffer = ByteBuffer.allocate(iv.size + encryptedBytes.size)
        byteBuffer.put(iv)
        byteBuffer.put(encryptedBytes)

        return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
    }

    /**
     * Mendekode thumbnail mikro terenkripsi menjadi Bitmap buram 16x16 px
     */
    fun decryptMicroThumbnail(encryptedThumbnailBase64: String, secretKey: SecretKeySpec): Bitmap? {
        return try {
            val fullBytes = Base64.decode(encryptedThumbnailBase64, Base64.NO_WRAP)
            val byteBuffer = ByteBuffer.wrap(fullBytes)
            val iv = ByteArray(GCM_IV_LENGTH)
            byteBuffer.get(iv)

            val cipherBytes = ByteArray(byteBuffer.remaining())
            byteBuffer.get(cipherBytes)

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedJpeg = cipher.doFinal(cipherBytes)
            BitmapFactory.decodeByteArray(decryptedJpeg, 0, decryptedJpeg.size)
        } catch (e: Exception) {
            null
        }
    }
}
