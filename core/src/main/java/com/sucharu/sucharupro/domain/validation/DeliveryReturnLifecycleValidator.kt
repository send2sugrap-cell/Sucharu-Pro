package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus

/**
 * Lifecycle state transition validator for Delivery Returns (Module 08 Step 07).
 */
object DeliveryReturnLifecycleValidator {

    private val validTransitions: Map<DeliveryReturnStatus, Set<DeliveryReturnStatus>> = mapOf(
        DeliveryReturnStatus.DRAFT to setOf(DeliveryReturnStatus.PENDING, DeliveryReturnStatus.CANCELLED),
        DeliveryReturnStatus.PENDING to setOf(DeliveryReturnStatus.APPROVED, DeliveryReturnStatus.REJECTED, DeliveryReturnStatus.CANCELLED),
        DeliveryReturnStatus.APPROVED to setOf(DeliveryReturnStatus.RECEIVING, DeliveryReturnStatus.RECEIVED, DeliveryReturnStatus.CANCELLED),
        DeliveryReturnStatus.RECEIVING to setOf(DeliveryReturnStatus.RECEIVED, DeliveryReturnStatus.CANCELLED),
        DeliveryReturnStatus.RECEIVED to setOf(DeliveryReturnStatus.INSPECTING, DeliveryReturnStatus.INSPECTED, DeliveryReturnStatus.CANCELLED),
        DeliveryReturnStatus.INSPECTING to setOf(DeliveryReturnStatus.INSPECTED),
        DeliveryReturnStatus.INSPECTED to setOf(DeliveryReturnStatus.DISPOSITION_PENDING, DeliveryReturnStatus.PROCESSING, DeliveryReturnStatus.COMPLETED),
        DeliveryReturnStatus.DISPOSITION_PENDING to setOf(DeliveryReturnStatus.PROCESSING, DeliveryReturnStatus.COMPLETED),
        DeliveryReturnStatus.PROCESSING to setOf(DeliveryReturnStatus.COMPLETED),
        DeliveryReturnStatus.COMPLETED to emptySet(),
        DeliveryReturnStatus.CANCELLED to emptySet(),
        DeliveryReturnStatus.REJECTED to emptySet()
    )

    fun validateTransition(from: DeliveryReturnStatus, to: DeliveryReturnStatus): DomainResult<Unit> {
        if (from == to) return DomainResult.Success(Unit)

        if (from.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition from terminal status '${from.defaultLabel}' to '${to.defaultLabel}'."
            )
        }

        val allowed = validTransitions[from] ?: emptySet()
        if (to !in allowed) {
            return DomainResult.Error(
                message = "Invalid Delivery Return transition from '${from.defaultLabel}' to '${to.defaultLabel}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}
