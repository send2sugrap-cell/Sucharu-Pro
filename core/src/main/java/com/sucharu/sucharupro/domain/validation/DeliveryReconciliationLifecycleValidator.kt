package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus

/**
 * State machine transition validator for Delivery Reconciliation (Module 08 Step 09).
 */
object DeliveryReconciliationLifecycleValidator {

    fun validateTransition(
        currentStatus: DeliveryReconciliationStatus,
        targetStatus: DeliveryReconciliationStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition reconciliation from terminal status '$currentStatus' to '$targetStatus'."
            )
        }

        val isValid = when (currentStatus) {
            DeliveryReconciliationStatus.OPEN -> targetStatus in listOf(
                DeliveryReconciliationStatus.IN_PROGRESS,
                DeliveryReconciliationStatus.PARTIALLY_RECONCILED,
                DeliveryReconciliationStatus.REQUIRES_REVIEW,
                DeliveryReconciliationStatus.RECONCILED,
                DeliveryReconciliationStatus.DISPUTED,
                DeliveryReconciliationStatus.CLOSED
            )

            DeliveryReconciliationStatus.IN_PROGRESS -> targetStatus in listOf(
                DeliveryReconciliationStatus.PARTIALLY_RECONCILED,
                DeliveryReconciliationStatus.REQUIRES_REVIEW,
                DeliveryReconciliationStatus.RECONCILED,
                DeliveryReconciliationStatus.DISPUTED,
                DeliveryReconciliationStatus.RESOLVED,
                DeliveryReconciliationStatus.CLOSED
            )

            DeliveryReconciliationStatus.PARTIALLY_RECONCILED -> targetStatus in listOf(
                DeliveryReconciliationStatus.IN_PROGRESS,
                DeliveryReconciliationStatus.REQUIRES_REVIEW,
                DeliveryReconciliationStatus.RECONCILED,
                DeliveryReconciliationStatus.DISPUTED,
                DeliveryReconciliationStatus.RESOLVED,
                DeliveryReconciliationStatus.CLOSED
            )

            DeliveryReconciliationStatus.REQUIRES_REVIEW -> targetStatus in listOf(
                DeliveryReconciliationStatus.IN_PROGRESS,
                DeliveryReconciliationStatus.DISPUTED,
                DeliveryReconciliationStatus.RESOLVED,
                DeliveryReconciliationStatus.RECONCILED,
                DeliveryReconciliationStatus.CLOSED
            )

            DeliveryReconciliationStatus.DISPUTED -> targetStatus in listOf(
                DeliveryReconciliationStatus.IN_PROGRESS,
                DeliveryReconciliationStatus.REQUIRES_REVIEW,
                DeliveryReconciliationStatus.RESOLVED,
                DeliveryReconciliationStatus.CLOSED
            )

            DeliveryReconciliationStatus.RESOLVED -> targetStatus in listOf(
                DeliveryReconciliationStatus.IN_PROGRESS,
                DeliveryReconciliationStatus.RECONCILED,
                DeliveryReconciliationStatus.CLOSED
            )

            DeliveryReconciliationStatus.RECONCILED -> targetStatus in listOf(
                DeliveryReconciliationStatus.IN_PROGRESS,
                DeliveryReconciliationStatus.DISPUTED,
                DeliveryReconciliationStatus.REQUIRES_REVIEW,
                DeliveryReconciliationStatus.CLOSED
            )

            DeliveryReconciliationStatus.CLOSED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal reconciliation status transition from '$currentStatus' to '$targetStatus'."
            )
        }
    }
}
