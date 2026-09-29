package com.example

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.ChatinViewModel
import com.example.util.BiometricHelper

/**
 * Aktivitas Utama Chatin Enterprise Solution.
 * Mewarisi FragmentActivity() untuk stabilitas mutlak BiometricPrompt sistem operasi Android
 * dan mencegah bug crash FragmentManager pada perangkat fisik maupun emulator.
 */
class MainActivity : FragmentActivity() {

    private val viewModel: ChatinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pelindung Crash Thread Global (mencegah dialog 'chatin telah berhenti' dari background thread/coroutine)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("CHATIN_CRASH_GUARD", "Mencegah force-close dari thread [${thread.name}]: ${throwable.localizedMessage}", throwable)
            if (thread.name.equals("main", ignoreCase = true)) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        enableEdgeToEdge()

        setContent {
            val themeState by viewModel.themeState.collectAsStateWithLifecycle()
            val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()

            // Peluncur izin runtime esensial untuk VoIP, Audio, Kontak & Notifikasi
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { _ ->
                // Handled gracefully tanpa memblokir pengalaman pengguna
            }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }

            val fontScale = authUiState.user?.fontSizeScale ?: 1.0f
            val displayScale = authUiState.user?.displayDensityScale ?: 1.0f
            val baseDensity = androidx.compose.ui.platform.LocalDensity.current
            val customDensity = remember(baseDensity, fontScale, displayScale) {
                androidx.compose.ui.unit.Density(
                    density = (baseDensity.density * displayScale).coerceIn(1.0f, 5.0f),
                    fontScale = (baseDensity.fontScale * fontScale).coerceIn(0.7f, 2.0f)
                )
            }

            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides customDensity) {
                MyApplicationTheme(themeMode = themeState.themeMode) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainAppScaffold(
                            viewModel = viewModel,
                            onTriggerBiometricAuth = { onBiometricSuccess ->
                                showBiometricSystemDialog(onBiometricSuccess)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Memeriksa ketersediaan sensor biometrik (sidik jari / wajah) bawaan perangkat keras
     */
    fun checkDeviceBiometricCapability(): Boolean {
        return BiometricHelper.checkDeviceBiometricCapability(this)
    }

    /**
     * Menampilkan dialog biometrik sistem operasi Android asli secara kriptografis
     * untuk membuka ruang obrolan sensitif atau kunci privasi
     */
    fun showBiometricSystemDialog(onBiometricSuccess: () -> Unit) {
        if (!checkDeviceBiometricCapability()) {
            Toast.makeText(this, "Sensor biometrik tidak tersedia atau belum didaftarkan di setelan ponsel. Gunakan PIN/Pola.", Toast.LENGTH_SHORT).show()
            // Buka secara fallback jika perangkat tidak memiliki sensor biometrik
            onBiometricSuccess()
            return
        }

        BiometricHelper.showBiometricSystemDialog(
            activity = this,
            title = "Buka Ruang Obrolan Rahasia",
            subtitle = "Pindai sidik jari atau pengenalan wajah Anda untuk mengakses obrolan sensitif",
            onSuccess = onBiometricSuccess,
            onError = { errorMsg ->
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.updateCallOptions { it.copy(isFloatingPipActive = isInPictureInPictureMode) }
    }
}
