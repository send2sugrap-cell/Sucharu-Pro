package com.sucharu.sucharupro.domain.validation.communication.internal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationStatus

/**
 * State machine validator for Internal Communication Lifecycles (Module 10 Step 03).
 */
object InternalCommunicationLifecycleValidator {

    private val allowedTransitions = mapOf(
        InternalCommunicationStatus.DRAFT to setOf(
            InternalCommunicationStatus.SCHEDULED,
            InternalCommunicationStatus.QUEUED,
            InternalCommunicationStatus.CANCELLED,
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.SCHEDULED to setOf(
            InternalCommunicationStatus.QUEUED,
            InternalCommunicationStatus.CANCELLED,
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.QUEUED to setOf(
            InternalCommunicationStatus.SENT,
            InternalCommunicationStatus.FAILED,
            InternalCommunicationStatus.CANCELLED,
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.SENT to setOf(
            InternalCommunicationStatus.DELIVERED,
            InternalCommunicationStatus.FAILED,
            InternalCommunicationStatus.READ,
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.DELIVERED to setOf(
            InternalCommunicationStatus.READ,
            InternalCommunicationStatus.ACKNOWLEDGED,
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.READ to setOf(
            InternalCommunicationStatus.ACKNOWLEDGED,
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.ACKNOWLEDGED to setOf(
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.FAILED to setOf(
            InternalCommunicationStatus.QUEUED,
            InternalCommunicationStatus.CANCELLED,
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.CANCELLED to setOf(
            InternalCommunicationStatus.ARCHIVED
        ),
        InternalCommunicationStatus.ARCHIVED to emptySet()
    )

    fun validateTransition(
        currentStatus: InternalCommunicationStatus,
        targetStatus: InternalCommunicationStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        val allowed = allowedTransitions[currentStatus] ?: emptySet()
        return if (allowed.contains(targetStatus)) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid internal communication lifecycle transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }
    }
}
