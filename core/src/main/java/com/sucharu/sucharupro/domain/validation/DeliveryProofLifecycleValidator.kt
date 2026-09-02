package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus

/**
 * State machine validator for DeliveryProof lifecycle transitions (Module 08 Step 08).
 */
object DeliveryProofLifecycleValidator {

    fun validateTransition(
        currentStatus: DeliveryProofStatus,
        targetStatus: DeliveryProofStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition POD from terminal status '$currentStatus' to '$targetStatus'."
            )
        }

        val isValid = when (currentStatus) {
            DeliveryProofStatus.DRAFT -> targetStatus in listOf(
                DeliveryProofStatus.PENDING_REVIEW,
                DeliveryProofStatus.SUBMITTED,
                DeliveryProofStatus.CANCELLED
            )
            DeliveryProofStatus.PENDING_REVIEW -> targetStatus in listOf(
                DeliveryProofStatus.SUBMITTED,
                DeliveryProofStatus.VERIFIED,
                DeliveryProofStatus.ACCEPTED,
                DeliveryProofStatus.REJECTED,
                DeliveryProofStatus.CANCELLED
            )
            DeliveryProofStatus.SUBMITTED -> targetStatus in listOf(
                DeliveryProofStatus.PENDING_REVIEW,
                DeliveryProofStatus.VERIFIED,
                DeliveryProofStatus.ACCEPTED,
                DeliveryProofStatus.REJECTED,
                DeliveryProofStatus.CANCELLED
            )
            DeliveryProofStatus.VERIFIED -> targetStatus in listOf(
                DeliveryProofStatus.ACCEPTED,
                DeliveryProofStatus.REJECTED
            )
            DeliveryProofStatus.REJECTED -> targetStatus in listOf(
                DeliveryProofStatus.DRAFT,
                DeliveryProofStatus.CANCELLED
            )
            DeliveryProofStatus.ACCEPTED,
            DeliveryProofStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal POD lifecycle transition from '$currentStatus' to '$targetStatus'."
            )
        }
    }
}
