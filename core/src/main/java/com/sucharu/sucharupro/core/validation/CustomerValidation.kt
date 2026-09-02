package com.sucharu.sucharupro.core.validation

/**
 * Standard validation logic for Customer contact information and profile fields.
 *
 * Implements robust validation rules for Bangladesh local and international formats:
 * - Phone numbers (supporting BD mobile 01X, +880, landlines, spaces, hyphens)
 * - Email addresses (optional, RFC standard regex when provided)
 * - Display names and Unicode text (Bangla, English, mixed)
 */
object CustomerValidation {

    /**
     * Regex pattern for standard email validation.
     */
    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    /**
     * Regex pattern for phone number characters (digits, +, -, (), spaces).
     */
    private val PHONE_ALLOWED_CHARS_REGEX = "^[0-9+\\-\\s()]+$".toRegex()

    /**
     * Validates customer display name.
     * Name is mandatory and must not be empty or whitespace only.
     */
    fun validateDisplayName(name: String): String? {
        val trimmed = name.trim()
        return if (trimmed.isBlank()) {
            "Customer name is required."
        } else if (trimmed.length < 2) {
            "Customer name must be at least 2 characters."
        } else {
            null
        }
    }

    /**
     * Validates primary phone number.
     * Primary phone is mandatory and must be a valid phone format.
     */
    fun validatePrimaryPhone(phone: String): String? {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) {
            return "Primary phone is required."
        }
        return validatePhoneFormat(trimmed, isRequired = true)
    }

    /**
     * Validates optional alternate phone number.
     * If blank, it is considered valid. If present, it must be a valid phone format.
     */
    fun validateAlternatePhone(phone: String): String? {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) {
            return null
        }
        return validatePhoneFormat(trimmed, isRequired = false)
    }

    /**
     * Validates phone format.
     * Accepts:
     * - Bangladesh mobile numbers: e.g. "01711234567", "+880 1711-234567", "01912-345678"
     * - Bangladesh landlines: e.g. "02-9876543", "+880-2-9876543"
     * - General international phone numbers with 6 to 15 digits.
     */
    private fun validatePhoneFormat(phone: String, isRequired: Boolean): String? {
        if (!PHONE_ALLOWED_CHARS_REGEX.matches(phone)) {
            return "Enter a valid phone number (e.g., +880 1711-234567 or 01711234567)."
        }
        // Count actual numeric digits
        val digitsOnly = phone.filter { it.isDigit() }
        if (digitsOnly.length < 6 || digitsOnly.length > 15) {
            return "Phone number must contain between 6 and 15 digits."
        }
        return null
    }

    /**
     * Validates email address.
     * Email is optional. If provided, it must conform to standard email syntax.
     */
    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return null
        }
        return if (EMAIL_REGEX.matches(trimmed)) {
            null
        } else {
            "Enter a valid email address (e.g., info@domain.com)."
        }
    }

    /**
     * Sanitizes and normalizes phone number for dialer actions (e.g. tel: URI).
     */
    fun sanitizeForDialer(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }

    /**
     * Normalizes a phone number for comparison and duplicate detection.
     * Strips non-digit formatting characters and standardizes Bangladesh +880 prefixes to local 01X.
     */
    fun normalizePhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.startsWith("880") && digits.length >= 13) {
            "0" + digits.substring(3)
        } else {
            digits
        }
    }

    /**
     * Normalizes an email address for comparison and duplicate detection.
     */
    fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    /**
     * Checks whether two customer contact sets are potential duplicates based on
     * normalized primary phone numbers or non-blank normalized emails.
     */
    fun isPotentialDuplicate(
        phoneA: String,
        emailA: String?,
        phoneB: String,
        emailB: String?
    ): Boolean {
        val normPhoneA = normalizePhoneNumber(phoneA)
        val normPhoneB = normalizePhoneNumber(phoneB)
        if (normPhoneA.isNotBlank() && normPhoneA == normPhoneB) {
            return true
        }

        val normEmailA = emailA?.trim()?.takeIf { it.isNotBlank() }?.let { normalizeEmail(it) }
        val normEmailB = emailB?.trim()?.takeIf { it.isNotBlank() }?.let { normalizeEmail(it) }
        if (normEmailA != null && normEmailB != null && normEmailA == normEmailB) {
            return true
        }

        return false
    }

    /**
     * Validates customer internal note text.
     * Note text is required, must not be blank or whitespace-only, and max 1000 characters.
     */
    fun validateNoteText(text: String): String? {
        val trimmed = text.trim()
        return if (trimmed.isBlank()) {
            "Note text cannot be empty."
        } else if (trimmed.length > 1000) {
            "Note text cannot exceed 1000 characters (currently ${trimmed.length})."
        } else {
            null
        }
    }
}
