package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus

/**
 * Enforces valid state transitions for Delivery Partial Settlements (Module 08 Step 06).
 */
object DeliverySettlementLifecycleValidator {

    fun validateTransition(
        currentStatus: DeliverySettlementStatus,
        targetStatus: DeliverySettlementStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) {
            return DomainResult.Success(Unit)
        }

        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition from terminal settlement status '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }

        val isValid = when (currentStatus) {
            DeliverySettlementStatus.OPEN -> targetStatus in listOf(
                DeliverySettlementStatus.PARTIALLY_DELIVERED,
                DeliverySettlementStatus.FULLY_DELIVERED,
                DeliverySettlementStatus.PARTIALLY_RETURNED,
                DeliverySettlementStatus.SETTLEMENT_PENDING,
                DeliverySettlementStatus.DISPUTED,
                DeliverySettlementStatus.CANCELLED
            )
            DeliverySettlementStatus.PARTIALLY_DELIVERED -> targetStatus in listOf(
                DeliverySettlementStatus.PARTIALLY_DELIVERED,
                DeliverySettlementStatus.FULLY_DELIVERED,
                DeliverySettlementStatus.PARTIALLY_RETURNED,
                DeliverySettlementStatus.SETTLEMENT_PENDING,
                DeliverySettlementStatus.DISPUTED,
                DeliverySettlementStatus.CANCELLED
            )
            DeliverySettlementStatus.FULLY_DELIVERED -> targetStatus in listOf(
                DeliverySettlementStatus.SETTLEMENT_PENDING,
                DeliverySettlementStatus.SETTLED,
                DeliverySettlementStatus.DISPUTED,
                DeliverySettlementStatus.PARTIALLY_RETURNED
            )
            DeliverySettlementStatus.PARTIALLY_RETURNED -> targetStatus in listOf(
                DeliverySettlementStatus.PARTIALLY_DELIVERED,
                DeliverySettlementStatus.FULLY_DELIVERED,
                DeliverySettlementStatus.SETTLEMENT_PENDING,
                DeliverySettlementStatus.DISPUTED
            )
            DeliverySettlementStatus.SETTLEMENT_PENDING -> targetStatus in listOf(
                DeliverySettlementStatus.SETTLED,
                DeliverySettlementStatus.DISPUTED,
                DeliverySettlementStatus.OPEN
            )
            DeliverySettlementStatus.DISPUTED -> targetStatus in listOf(
                DeliverySettlementStatus.OPEN,
                DeliverySettlementStatus.SETTLEMENT_PENDING,
                DeliverySettlementStatus.CANCELLED
            )
            DeliverySettlementStatus.SETTLED,
            DeliverySettlementStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid settlement status transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }
    }
}
