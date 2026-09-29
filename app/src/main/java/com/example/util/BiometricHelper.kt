package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Helper Otentikasi Biometrik Perangkat Keras Asli Android OS.
 * Menggunakan androidx.biometric.BiometricPrompt untuk pemindaian sidik jari dan pengenalan wajah fisik.
 */
object BiometricHelper {

    /**
     * Memeriksa kapabilitas sensor biometrik fisik pada perangkat
     */
    fun checkDeviceBiometricCapability(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Menampilkan dialog biometrik sistem operasi Android asli
     */
    fun showBiometricSystemDialog(
        activity: FragmentActivity,
        title: String = "Buka Ruang Obrolan Rahasia",
        subtitle: String = "Pindai sidik jari atau wajah Anda untuk melanjutkan",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Jika pengguna membatalkan (errorCode 10 atau 13), abaikan tanpa crash
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autentikasi biometrik tidak cocok")
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
