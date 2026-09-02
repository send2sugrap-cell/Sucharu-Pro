package com.sucharu.sucharupro.domain.model.communication.customer

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Validates and resolves customer ownership and recipient containment (Module 10 Step 02).
 */
object CustomerCommunicationRecipientResolver {

    data class ResolvedRecipient(
        val customerId: String,
        val recipientUserId: String,
        val projectId: String
    )

    fun resolve(
        projectId: String,
        customerId: String,
        recipientUserId: String? = null
    ): DomainResult<ResolvedRecipient> {
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }

        // If specific recipientUserId not supplied, default to customerId account
        val finalRecipientUserId = if (recipientUserId.isNullOrBlank()) customerId else recipientUserId

        return DomainResult.Success(
            ResolvedRecipient(
                customerId = customerId,
                recipientUserId = finalRecipientUserId,
                projectId = projectId
            )
        )
    }
}
