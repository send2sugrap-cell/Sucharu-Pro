package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentType

/**
 * Validates parameters for Vendor Document Requests (Module 10 Step 06).
 */
object VendorDocumentRequestValidator {

    data class RequestValidationError(val field: String, val message: String)

    fun validateCreate(
        projectId: String,
        vendorId: String,
        documentType: VendorDocumentType,
        title: String,
        dueDate: Long? = null,
        requestedBy: String,
        now: Long = System.currentTimeMillis()
    ): DomainResult<Unit> {
        val errors = mutableListOf<RequestValidationError>()

        if (projectId.isBlank()) errors.add(RequestValidationError("projectId", "Project ID cannot be blank."))
        if (vendorId.isBlank()) errors.add(RequestValidationError("vendorId", "Vendor ID cannot be blank."))
        if (title.isBlank()) errors.add(RequestValidationError("title", "Request title cannot be blank."))
        if (title.length > 200) errors.add(RequestValidationError("title", "Request title cannot exceed 200 characters."))
        if (requestedBy.isBlank()) errors.add(RequestValidationError("requestedBy", "Requester ID cannot be blank."))

        if (dueDate != null && dueDate < now) {
            errors.add(RequestValidationError("dueDate", "Due date cannot be in the past."))
        }

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Document request validation failed: ${errors.joinToString { "${it.field}: ${it.message}" }}")
        }
    }
}
