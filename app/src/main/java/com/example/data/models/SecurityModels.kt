package com.example.data.models

enum class BiometricLockStatus {
    ENABLED,
    DISABLED
}

enum class LockType(val label: String) {
    BIOMETRIC("Sidik Jari / Wajah"),
    PIN("PIN Angka"),
    PATTERN("Pola Kunci"),
    PASSWORD("Kata Sandi")
}

data class HiddenChatSecurity(
    val isSetup: Boolean = true,
    val lockType: LockType = LockType.BIOMETRIC,
    val secretCode: String = "1234",
    val hiddenChatIds: Set<String> = emptySet(),
    val isBiometricAvailable: Boolean = true,
    val biometricStatus: BiometricLockStatus = BiometricLockStatus.ENABLED
)

data class WebLinkedDevice(
    val id: String,
    val browserName: String,
    val osName: String,
    val location: String,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
