package com.example.util

/**
 * Validates phone numbers in E.164 format before requesting Firebase Phone Auth.
 * The country calling code must start with 1-9 and the total number must contain 8-15 digits.
 */
object PhoneNumberValidator {
    private val e164Pattern = Regex("^\\+[1-9]\\d{7,14}$")

    fun isValidE164(phoneNumber: String): Boolean =
        e164Pattern.matches(phoneNumber.trim())
}
