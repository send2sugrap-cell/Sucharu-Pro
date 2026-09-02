package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentType

/**
 * Validates field constraints and parameters for Vendor Document submission and updates (Module 10 Step 06).
 */
object VendorDocumentValidator {

    data class ValidationError(val field: String, val message: String)

    fun validateSubmission(
        projectId: String,
        vendorId: String,
        documentType: VendorDocumentType,
        title: String,
        fileReferenceId: String,
        issueDate: Long? = null,
        expiryDate: Long? = null,
        actorId: String
    ): DomainResult<Unit> {
        val errors = mutableListOf<ValidationError>()

        if (projectId.isBlank()) errors.add(ValidationError("projectId", "Project ID cannot be blank."))
        if (vendorId.isBlank()) errors.add(ValidationError("vendorId", "Vendor ID cannot be blank."))
        if (title.isBlank()) errors.add(ValidationError("title", "Document title cannot be blank."))
        if (title.length > 200) errors.add(ValidationError("title", "Document title cannot exceed 200 characters."))
        if (fileReferenceId.isBlank()) errors.add(ValidationError("fileReferenceId", "File reference cannot be blank upon submission."))
        if (actorId.isBlank()) errors.add(ValidationError("actorId", "Actor ID cannot be blank."))

        if (documentType.requiresExpiry && expiryDate == null) {
            errors.add(ValidationError("expiryDate", "Document type '${documentType.defaultLabel}' requires an expiry date."))
        }

        if (issueDate != null && expiryDate != null && expiryDate < issueDate) {
            errors.add(ValidationError("expiryDate", "Expiry date cannot be earlier than issue date."))
        }

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Document validation failed: ${errors.joinToString { "${it.field}: ${it.message}" }}")
        }
    }
}
