package com.sucharu.sucharupro.core.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerValidationTest {

    @Test
    fun test01_displayName_acceptsValidNames() {
        assertNull(CustomerValidation.validateDisplayName("Md. Abdullah Rahman"))
        assertNull(CustomerValidation.validateDisplayName("সোনার বাংলা প্রিন্টার্স"))
        assertNull(CustomerValidation.validateDisplayName("ABC Press & Packaging Ltd."))
    }

    @Test
    fun test02_displayName_rejectsBlankOrTooShort() {
        assertEquals("Customer name is required.", CustomerValidation.validateDisplayName(""))
        assertEquals("Customer name is required.", CustomerValidation.validateDisplayName("   "))
        assertEquals("Customer name must be at least 2 characters.", CustomerValidation.validateDisplayName("A"))
    }

    @Test
    fun test03_primaryPhone_acceptsValidBDFormats() {
        assertNull(CustomerValidation.validatePrimaryPhone("+880 1711-234567"))
        assertNull(CustomerValidation.validatePrimaryPhone("01711234567"))
        assertNull(CustomerValidation.validatePrimaryPhone("+8801819998877"))
        assertNull(CustomerValidation.validatePrimaryPhone("01912-345678"))
        assertNull(CustomerValidation.validatePrimaryPhone("02-9876543"))
        assertNull(CustomerValidation.validatePrimaryPhone("+880-2-9876543"))
    }

    @Test
    fun test04_primaryPhone_rejectsInvalidFormats() {
        assertEquals("Primary phone is required.", CustomerValidation.validatePrimaryPhone(""))
        assertEquals("Primary phone is required.", CustomerValidation.validatePrimaryPhone("   "))
        assertNotNull(CustomerValidation.validatePrimaryPhone("123")) // Too short
        assertNotNull(CustomerValidation.validatePrimaryPhone("01711-ABC-DEF")) // Non-digits
        assertNotNull(CustomerValidation.validatePrimaryPhone("abcdefghijk"))
    }

    @Test
    fun test05_alternatePhone_optionalBehavior() {
        assertNull(CustomerValidation.validateAlternatePhone(""))
        assertNull(CustomerValidation.validateAlternatePhone("   "))
        assertNull(CustomerValidation.validateAlternatePhone("+880 1611-000000"))
        assertNotNull(CustomerValidation.validateAlternatePhone("not-a-phone"))
    }

    @Test
    fun test06_email_optionalBehaviorAndValidation() {
        // Blank is allowed (optional)
        assertNull(CustomerValidation.validateEmail(""))
        assertNull(CustomerValidation.validateEmail("   "))

        // Valid emails
        assertNull(CustomerValidation.validateEmail("info@sucharupro.com"))
        assertNull(CustomerValidation.validateEmail("user.name+tag@example.co.uk"))

        // Invalid emails
        assertNotNull(CustomerValidation.validateEmail("invalid-email"))
        assertNotNull(CustomerValidation.validateEmail("user@"))
        assertNotNull(CustomerValidation.validateEmail("@example.com"))
        assertNotNull(CustomerValidation.validateEmail("user@example"))
    }

    @Test
    fun test07_sanitizeForDialer() {
        assertEquals("+8801711234567", CustomerValidation.sanitizeForDialer("+880 1711-234567"))
        assertEquals("01711234567", CustomerValidation.sanitizeForDialer("01711-234-567"))
        assertEquals("029876543", CustomerValidation.sanitizeForDialer("(02) 987-6543"))
    }

    @Test
    fun test08_validateNoteText_acceptsValidText() {
        assertNull(CustomerValidation.validateNoteText("গ্রাহক বইয়ের প্রুফ সরাসরি শোরুমে এসে চেক করতে পছন্দ করেন।"))
        assertNull(CustomerValidation.validateNoteText("Prefers 80 GSM art paper and delivery before noon."))
        assertNull(CustomerValidation.validateNoteText("123 Special instruction: Call 01711-000000 before delivery."))
    }

    @Test
    fun test09_validateNoteText_rejectsBlankAndTooLong() {
        assertEquals("Note text cannot be empty.", CustomerValidation.validateNoteText(""))
        assertEquals("Note text cannot be empty.", CustomerValidation.validateNoteText("     "))

        val tooLong = "A".repeat(1001)
        assertNotNull(CustomerValidation.validateNoteText(tooLong))
    }

    @Test
    fun test10_normalizePhoneAndEmail() {
        assertEquals("01711234567", CustomerValidation.normalizePhoneNumber("+880 1711-234567"))
        assertEquals("01711234567", CustomerValidation.normalizePhoneNumber("+8801711234567"))
        assertEquals("01711234567", CustomerValidation.normalizePhoneNumber("01711-234567"))
        assertEquals("029876543", CustomerValidation.normalizePhoneNumber("02-9876543"))

        assertEquals("info@sucharupro.com", CustomerValidation.normalizeEmail("  INFO@SucharuPro.COM  "))
    }

    @Test
    fun test11_isPotentialDuplicate() {
        // Same BD phone with different formatting
        org.junit.Assert.assertTrue(
            CustomerValidation.isPotentialDuplicate(
                phoneA = "+880 1711-234567",
                emailA = null,
                phoneB = "01711234567",
                emailB = null
            )
        )

        // Same email with different casing/spaces
        org.junit.Assert.assertTrue(
            CustomerValidation.isPotentialDuplicate(
                phoneA = "01811000000",
                emailA = "test@domain.com",
                phoneB = "01922000000",
                emailB = "  TEST@DOMAIN.COM "
            )
        )

        // Different contacts
        org.junit.Assert.assertFalse(
            CustomerValidation.isPotentialDuplicate(
                phoneA = "01711000000",
                emailA = "author@book.com",
                phoneB = "01811000000",
                emailB = "publisher@press.com"
            )
        )
    }
}
