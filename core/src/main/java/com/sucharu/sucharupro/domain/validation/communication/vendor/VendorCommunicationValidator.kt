package com.sucharu.sucharupro.domain.validation.communication.vendor

import com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Validates field-level constraints for VendorCommunication creation and update (Module 10 Step 05).
 */
object VendorCommunicationValidator {

    data class ValidationError(val field: String, val message: String)

    fun validate(
        projectId: String,
        vendorId: String,
        communicationType: String,
        subject: String,
        message: String,
        createdBy: String,
        scheduledAt: Long? = null,
        referenceType: String? = null,
        referenceId: String? = null
    ): DomainResult<Unit> {
        val errors = mutableListOf<ValidationError>()

        if (projectId.isBlank()) errors += ValidationError("projectId", "Project ID cannot be blank.")
        if (vendorId.isBlank()) errors += ValidationError("vendorId", "Vendor ID cannot be blank.")
        if (communicationType.isBlank()) errors += ValidationError("communicationType", "Communication type cannot be blank.")
        if (subject.isBlank()) errors += ValidationError("subject", "Subject cannot be blank.")
        if (subject.length > 250) errors += ValidationError("subject", "Subject cannot exceed 250 characters.")
        if (message.isBlank()) errors += ValidationError("message", "Message cannot be blank.")
        if (message.length > 10_000) errors += ValidationError("message", "Message cannot exceed 10,000 characters.")
        if (createdBy.isBlank()) errors += ValidationError("createdBy", "Created By cannot be blank.")

        if (scheduledAt != null && scheduledAt <= System.currentTimeMillis()) {
            errors += ValidationError("scheduledAt", "Scheduled time must be in the future.")
        }

        // Cross-field: referenceId requires referenceType
        if (!referenceId.isNullOrBlank() && referenceType.isNullOrBlank()) {
            errors += ValidationError("referenceType", "Reference Type is required when Reference ID is provided.")
        }

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = errors.joinToString("; ") { "${it.field}: ${it.message}" })
        }
    }

    fun validateDraftUpdate(
        subject: String?,
        message: String?
    ): DomainResult<Unit> {
        val errors = mutableListOf<ValidationError>()

        if (subject != null) {
            if (subject.isBlank()) errors += ValidationError("subject", "Subject cannot be blank.")
            if (subject.length > 250) errors += ValidationError("subject", "Subject cannot exceed 250 characters.")
        }
        if (message != null) {
            if (message.isBlank()) errors += ValidationError("message", "Message cannot be blank.")
            if (message.length > 10_000) errors += ValidationError("message", "Message cannot exceed 10,000 characters.")
        }

        return if (errors.isEmpty()) DomainResult.Success(Unit)
        else DomainResult.Error(message = errors.joinToString("; ") { "${it.field}: ${it.message}" })
    }

    fun validateAcknowledgement(
        communicationId: String,
        vendorId: String,
        projectId: String,
        acknowledgedBy: String,
        currentStatus: VendorCommunicationStatus
    ): DomainResult<Unit> {
        if (communicationId.isBlank()) return DomainResult.Error(message = "Communication ID cannot be blank.")
        if (vendorId.isBlank()) return DomainResult.Error(message = "Vendor ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (acknowledgedBy.isBlank()) return DomainResult.Error(message = "Acknowledged By cannot be blank.")
        if (currentStatus.isTerminal) {
            return DomainResult.Error(message = "Cannot acknowledge a communication in terminal state: $currentStatus.")
        }
        return DomainResult.Success(Unit)
    }
}
