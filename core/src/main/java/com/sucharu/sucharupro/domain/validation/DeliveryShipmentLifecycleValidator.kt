package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus

/**
 * Validates lifecycle state transitions for Delivery Shipments (Module 08 Step 05).
 */
object DeliveryShipmentLifecycleValidator {

    private val allowedTransitions: Map<DeliveryShipmentStatus, Set<DeliveryShipmentStatus>> = mapOf(
        DeliveryShipmentStatus.DRAFT to setOf(
            DeliveryShipmentStatus.READY,
            DeliveryShipmentStatus.CANCELLED
        ),
        DeliveryShipmentStatus.READY to setOf(
            DeliveryShipmentStatus.DISPATCHED,
            DeliveryShipmentStatus.CANCELLED
        ),
        DeliveryShipmentStatus.DISPATCHED to setOf(
            DeliveryShipmentStatus.IN_TRANSIT,
            DeliveryShipmentStatus.DELAYED,
            DeliveryShipmentStatus.ON_HOLD,
            DeliveryShipmentStatus.CANCELLED
        ),
        DeliveryShipmentStatus.IN_TRANSIT to setOf(
            DeliveryShipmentStatus.OUT_FOR_DELIVERY,
            DeliveryShipmentStatus.DELAYED,
            DeliveryShipmentStatus.ON_HOLD
        ),
        DeliveryShipmentStatus.OUT_FOR_DELIVERY to setOf(
            DeliveryShipmentStatus.DELIVERED,
            DeliveryShipmentStatus.DELIVERY_ATTEMPTED,
            DeliveryShipmentStatus.DELAYED,
            DeliveryShipmentStatus.ON_HOLD
        ),
        DeliveryShipmentStatus.DELIVERY_ATTEMPTED to setOf(
            DeliveryShipmentStatus.OUT_FOR_DELIVERY,
            DeliveryShipmentStatus.DELAYED,
            DeliveryShipmentStatus.ON_HOLD,
            DeliveryShipmentStatus.DELIVERED
        ),
        DeliveryShipmentStatus.DELAYED to setOf(
            DeliveryShipmentStatus.IN_TRANSIT,
            DeliveryShipmentStatus.OUT_FOR_DELIVERY,
            DeliveryShipmentStatus.ON_HOLD,
            DeliveryShipmentStatus.CANCELLED
        ),
        DeliveryShipmentStatus.ON_HOLD to setOf(
            DeliveryShipmentStatus.IN_TRANSIT,
            DeliveryShipmentStatus.OUT_FOR_DELIVERY,
            DeliveryShipmentStatus.CANCELLED
        ),
        DeliveryShipmentStatus.DELIVERED to emptySet(),
        DeliveryShipmentStatus.CANCELLED to emptySet()
    )

    fun validateTransition(
        currentStatus: DeliveryShipmentStatus,
        targetStatus: DeliveryShipmentStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) {
            return DomainResult.Success(Unit)
        }

        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Shipment is in terminal state '${currentStatus.defaultLabel}' and cannot be transitioned to '${targetStatus.defaultLabel}'."
            )
        }

        val validTargets = allowedTransitions[currentStatus] ?: emptySet()
        return if (targetStatus in validTargets) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal shipment status transition: Cannot transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }
    }
}
