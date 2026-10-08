package com.example

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.screens.LoginPhoneScreen // <- BIKIN SCREEN INI
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.ChatinViewModel
import com.example.util.BiometricHelper
import com.google.firebase.auth.FirebaseAuth

class MainActivity : FragmentActivity() {

    private val viewModel: ChatinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeState by viewModel.themeState.collectAsStateWithLifecycle()
            val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
            val auth = FirebaseAuth.getInstance()
            val isLoggedIn = auth.currentUser != null || authUiState.user != null

            MyApplicationTheme(themeMode = themeState.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isLoggedIn) {
                        // KAYAK WHATSAPP - WAJIB LOGIN DULU, GAK LANGSUNG MASUK OBROLAN
                        LoginPhoneScreen(
                            onLoginSuccess = { 
                                // abis login sukses, bakal otomatis ke MainAppScaffold
                            }
                        )
                    } else {
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

    fun checkDeviceBiometricCapability(): Boolean {
        return BiometricHelper.checkDeviceBiometricCapability(this)
    }

    fun showBiometricSystemDialog(onBiometricSuccess: () -> Unit) {
        if (!checkDeviceBiometricCapability()) {
            Toast.makeText(this, "Sensor biometrik tidak tersedia", Toast.LENGTH_SHORT).show()
            onBiometricSuccess()
            return
        }
        BiometricHelper.showBiometricSystemDialog(
            activity = this,
            title = "Buka Ruang Obrolan Rahasia",
            subtitle = "Pindai sidik jari Anda",
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
