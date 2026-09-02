package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.VendorContact

/**
 * Domain validator for VendorContact entity (Module 12 Step 02).
 */
object VendorContactValidator {

    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    private val PHONE_REGEX = "^[+0-9()\\s-]{6,30}$".toRegex()

    fun validate(contact: VendorContact): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (contact.contactId.isBlank()) {
            errors.add("contactId cannot be blank")
        }
        if (contact.vendorId.isBlank()) {
            errors.add("vendorId cannot be blank")
        }
        if (contact.projectId.isBlank()) {
            errors.add("projectId cannot be blank")
        }
        if (contact.name.isBlank()) {
            errors.add("name cannot be blank")
        } else if (contact.name.length > 150) {
            errors.add("name exceeds maximum length of 150 characters")
        }

        contact.designation?.let {
            if (it.length > 100) errors.add("designation exceeds maximum length of 100 characters")
        }

        contact.phone?.let {
            if (it.isNotBlank() && !PHONE_REGEX.matches(it)) {
                errors.add("phone must match valid phone number format")
            }
        }

        contact.alternatePhone?.let {
            if (it.isNotBlank() && !PHONE_REGEX.matches(it)) {
                errors.add("alternatePhone must match valid phone number format")
            }
        }

        contact.email?.let {
            if (it.isNotBlank() && !EMAIL_REGEX.matches(it)) {
                errors.add("email must match valid email address format")
            }
        }

        contact.notes?.let {
            if (it.length > 1000) errors.add("notes exceed maximum length of 1000 characters")
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }
}
