package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberValidatorTest {

    @Test
    fun acceptsValidIndonesianE164Number() {
        assertTrue(PhoneNumberValidator.isValidE164("+6281234567890"))
    }

    @Test
    fun rejectsLocalNumberWithoutCountryCode() {
        assertFalse(PhoneNumberValidator.isValidE164("081234567890"))
    }

    @Test
    fun rejectsNumberWithWhitespace() {
        assertFalse(PhoneNumberValidator.isValidE164("+62 81234567890"))
    }

    @Test
    fun rejectsTooShortOrTooLongNumbers() {
        assertFalse(PhoneNumberValidator.isValidE164("+1234567"))
        assertFalse(PhoneNumberValidator.isValidE164("+1234567890123456"))
    }

    @Test
    fun trimsOuterWhitespace() {
        assertTrue(PhoneNumberValidator.isValidE164("  +6281234567890  "))
    }
}
