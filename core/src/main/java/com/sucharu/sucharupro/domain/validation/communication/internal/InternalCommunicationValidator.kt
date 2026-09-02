package com.sucharu.sucharupro.domain.validation.communication.internal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunication
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType

/**
 * Structural, safety, and recipient validation for internal communications (Module 10 Step 03).
 */
object InternalCommunicationValidator {

    private val forbiddenKeywords = listOf(
        "password", "token", "secret", "cvv", "card_number", "pin", "api_key", "bearer", "private_key"
    )

    fun validate(communication: InternalCommunication): DomainResult<Unit> {
        if (communication.communicationId.isBlank()) {
            return DomainResult.Error(message = "Communication ID cannot be blank.")
        }
        if (communication.communicationNo.isBlank()) {
            return DomainResult.Error(message = "Communication Number cannot be blank.")
        }
        if (communication.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (communication.senderUserId.isBlank()) {
            return DomainResult.Error(message = "Sender User ID cannot be blank.")
        }
        if (communication.subject.isBlank()) {
            return DomainResult.Error(message = "Subject cannot be blank.")
        }
        if (communication.subject.length > 250) {
            return DomainResult.Error(message = "Subject exceeds maximum allowed length (250 chars).")
        }
        if (communication.message.isBlank()) {
            return DomainResult.Error(message = "Message body cannot be blank.")
        }
        if (communication.createdAt <= 0) {
            return DomainResult.Error(message = "Creation timestamp must be positive.")
        }
        if (communication.updatedAt < communication.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot precede creation timestamp.")
        }

        // Recipient target check
        when (communication.recipientType) {
            InternalCommunicationRecipientType.USER -> {
                if (communication.recipientUserIds.isEmpty()) {
                    return DomainResult.Error(message = "Direct communication requires at least one recipient user ID.")
                }
            }
            InternalCommunicationRecipientType.ROLE -> {
                if (communication.recipientRole == null) {
                    return DomainResult.Error(message = "Role-based communication requires a target role.")
                }
            }
            InternalCommunicationRecipientType.TEAM -> {
                if (communication.teamId.isNullOrBlank()) {
                    return DomainResult.Error(message = "Team communication requires a target team ID.")
                }
            }
            InternalCommunicationRecipientType.DEPARTMENT -> {
                if (communication.departmentId.isNullOrBlank()) {
                    return DomainResult.Error(message = "Department communication requires a target department ID.")
                }
            }
            InternalCommunicationRecipientType.PROJECT,
            InternalCommunicationRecipientType.ALL_INTERNAL_USERS -> Unit
        }

        // Security check
        val lowerSubj = communication.subject.lowercase()
        val lowerMsg = communication.message.lowercase()
        for (kw in forbiddenKeywords) {
            if (lowerSubj.contains(kw) || lowerMsg.contains(kw)) {
                return DomainResult.Error(message = "Message contains unauthorized security keyword '$kw'.")
            }
        }

        return DomainResult.Success(Unit)
    }
}
