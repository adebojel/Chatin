package com.example.data.models

data class UserAccount(
    val id: Int = 1,
    val username: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val bio: String = "Ada di Chatin • Komunikasi aman All-in-One",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val avatarColorHex: Long = 0xFF00E5FF,
    val isPhoneVerified: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val cloudSyncStatus: String = "Tersinkronisasi ke Server Cloud Chatin",
    val lastCloudBackupTime: Long = System.currentTimeMillis(),
    val googleAccountEmail: String = "",
    val lastGoogleBackupTime: Long = 0L,
    val privacyLastSeen: String = "Semua Orang",
    val privacyReadReceipts: Boolean = true,
    val mediaAutoDownloadWifi: Boolean = true,
    val mediaAutoDownloadCellular: Boolean = false,
    val fontSizeScale: Float = 1.0f,
    val displayDensityScale: Float = 1.0f,
    val highContrastMode: Boolean = false
)

enum class AuthStep {
    LOGIN,
    REGISTER,
    VERIFY_OTP,
    PROFILE_SETUP,
    AUTHENTICATED
}

data class AuthUiState(
    val currentStep: AuthStep = AuthStep.LOGIN,
    val user: UserAccount? = null,
    val jwtToken: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastSentOtp: String = "",
    val otpCooldownSeconds: Int = 60,
    val pendingPhoneNumber: String = "",
    val pendingUsername: String = ""
)
