package com.sucharu.sucharupro.ui.auth

import com.sucharu.sucharupro.core.validation.CustomerValidation
import com.sucharu.sucharupro.ui.features.auth.components.ContactPhoneOption
import org.junit.Assert.*
import org.junit.Test

/**
 * Verification of Phone Normalization and Contact Option Processing.
 */
class ContactPickerAndValidationTest {

    @Test
    fun test01_bangladeshPhoneNormalization() {
        assertEquals("01711234567", CustomerValidation.normalizePhoneNumber("+880 1711-234567"))
        assertEquals("01711234567", CustomerValidation.normalizePhoneNumber("+8801711234567"))
        assertEquals("01711234567", CustomerValidation.normalizePhoneNumber("01711-234567"))
        assertEquals("01711234567", CustomerValidation.normalizePhoneNumber("8801711234567"))
        assertEquals("01912345678", CustomerValidation.normalizePhoneNumber("+880 1912 345 678"))
    }

    @Test
    fun test02_contactPhoneOptionCreationAndDeduplication() {
        val rawNumbers = listOf(
            "+880 1711-234567",
            "01711234567",
            "01812345678"
        )

        val options = mutableListOf<ContactPhoneOption>()
        for (raw in rawNumbers) {
            val normalized = CustomerValidation.normalizePhoneNumber(raw)
            val option = ContactPhoneOption(
                label = "Mobile",
                rawNumber = raw,
                normalizedNumber = normalized
            )
            if (!options.any { it.normalizedNumber == option.normalizedNumber }) {
                options.add(option)
            }
        }

        // The first two numbers are duplicates when normalized
        assertEquals(2, options.size)
        assertEquals("01711234567", options[0].normalizedNumber)
        assertEquals("01812345678", options[1].normalizedNumber)
    }

    @Test
    fun test03_emailValidation() {
        assertNull(CustomerValidation.validateEmail("test@example.com"))
        assertNull(CustomerValidation.validateEmail("user.name+tag@sub.domain.org"))
        assertNull(CustomerValidation.validateEmail("")) // Optional email allows blank

        assertNotNull(CustomerValidation.validateEmail("invalid-email"))
        assertNotNull(CustomerValidation.validateEmail("user@domain"))
    }

    @Test
    fun test04_primaryPhoneValidation() {
        assertNull(CustomerValidation.validatePrimaryPhone("01711234567"))
        assertNull(CustomerValidation.validatePrimaryPhone("+880 1711-234567"))

        assertNotNull(CustomerValidation.validatePrimaryPhone(""))
        assertNotNull(CustomerValidation.validatePrimaryPhone("123")) // Too short
        assertNotNull(CustomerValidation.validatePrimaryPhone("abcdefghij")) // Non-digits
    }

    @Test
    fun test05_displayNameValidation() {
        assertNull(CustomerValidation.validateDisplayName("Rahim Ahmed"))
        assertNull(CustomerValidation.validateDisplayName("রহিম আহমেদ"))

        assertNotNull(CustomerValidation.validateDisplayName(""))
        assertNotNull(CustomerValidation.validateDisplayName("A")) // Too short
    }
}
