package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Helper Otentikasi Biometrik Perangkat Keras Asli Android OS.
 * Kompatibel dengan Android 9 (Pie), 10 (Q), 11 (R), 12, 13, 14, 15+ tanpa crash.
 */
object BiometricHelper {

    /**
     * Memeriksa kapabilitas sensor biometrik fisik pada perangkat secara aman
     */
    fun checkDeviceBiometricCapability(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
                biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
            }
        } catch (e: Throwable) {
            Log.w("BiometricHelper", "Pemeriksaan biometrik perangkat aman: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Menampilkan dialog biometrik sistem operasi Android asli
     * Dilengkapi pelindung crash parameter PromptInfo untuk beragam varian OS Android
     */
    fun showBiometricSystemDialog(
        activity: FragmentActivity,
        title: String = "Buka Ruang Obrolan Rahasia",
        subtitle: String = "Pindai sidik jari atau wajah Anda untuk melanjutkan",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val builder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ mendukung kombinasi BIOMETRIC_STRONG atau PIN/Pola perangkat
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                // Android 10 kebawah memerlukan Negative Button wajib jika kredensial perangkat tidak diaktifkan
                builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                builder.setNegativeButtonText("Batal")
            }

            val promptInfo = builder.build()

            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        activity.runOnUiThread {
                            onSuccess()
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Jika dibatalkan oleh pengguna, tidak dianggap crash atau error kritis
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            activity.runOnUiThread {
                                onError(errString.toString())
                            }
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        activity.runOnUiThread {
                            onError("Sidik jari tidak cocok. Silakan coba kembali.")
                        }
                    }
                }
            )

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            Log.e("BiometricHelper", "Gagal memulai dialog biometrik: ${e.localizedMessage}")
            onError("Sistem biometrik perangkat dialihkan ke PIN/Pola: ${e.localizedMessage}")
        }
    }
}
