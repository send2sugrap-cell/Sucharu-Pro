package com.sucharu.sucharupro.domain.validation.communication.customer

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunicationStatus

/**
 * State transition validator for customer communication lifecycles (Module 10 Step 02).
 */
object CustomerCommunicationLifecycleValidator {

    private val allowedTransitions = mapOf(
        CustomerCommunicationStatus.DRAFT to setOf(
            CustomerCommunicationStatus.SCHEDULED,
            CustomerCommunicationStatus.QUEUED,
            CustomerCommunicationStatus.CANCELLED
        ),
        CustomerCommunicationStatus.SCHEDULED to setOf(
            CustomerCommunicationStatus.QUEUED,
            CustomerCommunicationStatus.CANCELLED
        ),
        CustomerCommunicationStatus.QUEUED to setOf(
            CustomerCommunicationStatus.SENT,
            CustomerCommunicationStatus.DELIVERED,
            CustomerCommunicationStatus.FAILED,
            CustomerCommunicationStatus.CANCELLED
        ),
        CustomerCommunicationStatus.SENT to setOf(
            CustomerCommunicationStatus.DELIVERED,
            CustomerCommunicationStatus.FAILED,
            CustomerCommunicationStatus.READ
        ),
        CustomerCommunicationStatus.DELIVERED to setOf(
            CustomerCommunicationStatus.READ,
            CustomerCommunicationStatus.ACKNOWLEDGED
        ),
        CustomerCommunicationStatus.READ to setOf(
            CustomerCommunicationStatus.ACKNOWLEDGED
        ),
        CustomerCommunicationStatus.FAILED to setOf(
            CustomerCommunicationStatus.QUEUED,
            CustomerCommunicationStatus.CANCELLED
        ),
        CustomerCommunicationStatus.ACKNOWLEDGED to emptySet(),
        CustomerCommunicationStatus.CANCELLED to emptySet()
    )

    fun validateTransition(
        currentStatus: CustomerCommunicationStatus,
        targetStatus: CustomerCommunicationStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        val allowed = allowedTransitions[currentStatus] ?: emptySet()
        return if (allowed.contains(targetStatus)) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid customer communication lifecycle transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }
    }
}
