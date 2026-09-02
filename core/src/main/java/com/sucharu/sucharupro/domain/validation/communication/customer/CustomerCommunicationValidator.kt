package com.sucharu.sucharupro.domain.validation.communication.customer

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunication

/**
 * Structural and security integrity validator for customer communications (Module 10 Step 02).
 */
object CustomerCommunicationValidator {

    private val forbiddenKeywords = listOf(
        "password", "token", "secret", "cvv", "card_number", "pin", "api_key", "bearer", "private_key"
    )

    fun validate(communication: CustomerCommunication): DomainResult<Unit> {
        if (communication.communicationId.isBlank()) {
            return DomainResult.Error(message = "Communication ID cannot be blank.")
        }
        if (communication.communicationNo.isBlank()) {
            return DomainResult.Error(message = "Communication Number cannot be blank.")
        }
        if (communication.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (communication.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        if (communication.recipientUserId.isBlank()) {
            return DomainResult.Error(message = "Recipient User ID cannot be blank.")
        }
        if (communication.notificationId.isBlank()) {
            return DomainResult.Error(message = "Canonical Notification ID cannot be blank.")
        }
        if (communication.title.isBlank()) {
            return DomainResult.Error(message = "Communication title cannot be blank.")
        }
        if (communication.message.isBlank()) {
            return DomainResult.Error(message = "Communication message cannot be blank.")
        }
        if (communication.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created By cannot be blank.")
        }
        if (communication.createdAt <= 0) {
            return DomainResult.Error(message = "Creation timestamp must be positive.")
        }
        if (communication.updatedAt < communication.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot precede creation timestamp.")
        }

        // Verify metadata safety
        for ((k, v) in communication.metadata) {
            val lowerK = k.lowercase()
            val lowerV = v.lowercase()
            for (forbidden in forbiddenKeywords) {
                if (lowerK.contains(forbidden) || lowerV.contains(forbidden)) {
                    return DomainResult.Error(
                        message = "Sensitive data '$forbidden' is strictly prohibited in customer communication metadata."
                    )
                }
            }
        }

        return DomainResult.Success(Unit)
    }
}
