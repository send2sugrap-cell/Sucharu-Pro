package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus

/**
 * Result of vendor input validation.
 */
data class VendorValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    val errorMessage: String? get() = if (errors.isNotEmpty()) errors.joinToString("; ") else null
}

/**
 * Domain-level validator enforcing business constraints on Vendor master entities (Module 12 Step 01).
 */
object VendorValidator {

    private const val MAX_NAME_LENGTH = 200
    private const val MAX_CODE_LENGTH = 64
    private const val MAX_PHONE_LENGTH = 50
    private const val MAX_EMAIL_LENGTH = 100
    private const val MAX_NOTES_LENGTH = 2000

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun validate(vendor: Vendor): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (vendor.vendorId.isBlank()) {
            errors.add("vendorId is required and cannot be blank")
        }
        if (vendor.projectId.isBlank()) {
            errors.add("projectId is required and cannot be blank")
        }

        val trimmedName = vendor.vendorName.trim()
        if (trimmedName.isBlank()) {
            errors.add("vendorName is required and cannot be blank")
        } else if (trimmedName.length > MAX_NAME_LENGTH) {
            errors.add("vendorName exceeds maximum length of $MAX_NAME_LENGTH characters")
        }

        val trimmedCode = vendor.vendorCode.trim()
        if (trimmedCode.isBlank()) {
            errors.add("vendorCode is required and cannot be blank")
        } else if (trimmedCode.length > MAX_CODE_LENGTH) {
            errors.add("vendorCode exceeds maximum length of $MAX_CODE_LENGTH characters")
        }

        vendor.legalName?.let {
            if (it.length > MAX_NAME_LENGTH) {
                errors.add("legalName exceeds maximum length of $MAX_NAME_LENGTH characters")
            }
        }

        vendor.primaryPhone?.let {
            val phone = it.trim()
            if (phone.length > MAX_PHONE_LENGTH) {
                errors.add("primaryPhone exceeds maximum length of $MAX_PHONE_LENGTH characters")
            }
        }

        vendor.primaryEmail?.let {
            val email = it.trim()
            if (email.isNotBlank()) {
                if (email.length > MAX_EMAIL_LENGTH) {
                    errors.add("primaryEmail exceeds maximum length of $MAX_EMAIL_LENGTH characters")
                } else if (!EMAIL_REGEX.matches(email)) {
                    errors.add("primaryEmail is not a valid email address format")
                }
            }
        }

        vendor.notes?.let {
            if (it.length > MAX_NOTES_LENGTH) {
                errors.add("notes exceeds maximum length of $MAX_NOTES_LENGTH characters")
            }
        }

        if (vendor.version < 1L) {
            errors.add("version must be at least 1")
        }

        return VendorValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    fun validateStatusTransition(currentStatus: VendorStatus, newStatus: VendorStatus): VendorValidationResult {
        if (currentStatus == newStatus) {
            return VendorValidationResult(isValid = true)
        }

        if (currentStatus == VendorStatus.ARCHIVED) {
            return VendorValidationResult(
                isValid = false,
                errors = listOf("Cannot transition status from ARCHIVED state")
            )
        }

        return VendorValidationResult(isValid = true)
    }
}
