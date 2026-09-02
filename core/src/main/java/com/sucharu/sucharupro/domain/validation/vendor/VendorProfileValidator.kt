package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.VendorProfile

/**
 * Domain validator for VendorProfile entity (Module 12 Step 02).
 */
object VendorProfileValidator {

    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    private val PHONE_REGEX = "^[+0-9()\\s-]{6,30}$".toRegex()

    fun validate(profile: VendorProfile): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (profile.vendorId.isBlank()) {
            errors.add("vendorId cannot be blank")
        }
        if (profile.projectId.isBlank()) {
            errors.add("projectId cannot be blank")
        }
        if (profile.displayName.isBlank()) {
            errors.add("displayName cannot be blank")
        } else if (profile.displayName.length > 200) {
            errors.add("displayName exceeds maximum length of 200 characters")
        }

        profile.legalName?.let {
            if (it.length > 255) errors.add("legalName exceeds maximum length of 255 characters")
        }

        profile.contactPerson?.let {
            if (it.length > 150) errors.add("contactPerson exceeds maximum length of 150 characters")
        }

        profile.primaryPhone?.let {
            if (it.isNotBlank() && !PHONE_REGEX.matches(it)) {
                errors.add("primaryPhone must match valid phone number format")
            }
        }

        profile.alternatePhone?.let {
            if (it.isNotBlank() && !PHONE_REGEX.matches(it)) {
                errors.add("alternatePhone must match valid phone number format")
            }
        }

        profile.email?.let {
            if (it.isNotBlank() && !EMAIL_REGEX.matches(it)) {
                errors.add("email must match valid email address format")
            }
        }

        profile.website?.let {
            if (it.length > 255) errors.add("website exceeds maximum length of 255 characters")
        }

        profile.taxId?.let {
            if (it.length > 100) errors.add("taxId exceeds maximum length of 100 characters")
        }

        profile.businessRegistrationNumber?.let {
            if (it.length > 100) errors.add("businessRegistrationNumber exceeds maximum length of 100 characters")
        }

        profile.notes?.let {
            if (it.length > 2000) errors.add("notes exceed maximum length of 2000 characters")
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }
}
