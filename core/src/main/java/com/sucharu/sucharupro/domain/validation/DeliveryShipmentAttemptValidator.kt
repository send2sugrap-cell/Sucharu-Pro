package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttempt
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus

/**
 * Validates delivery attempt records and sequential integrity (Module 08 Step 05).
 */
object DeliveryShipmentAttemptValidator {

    fun validateAttempt(
        shipment: DeliveryShipment,
        attempt: DeliveryShipmentAttempt,
        existingAttempts: List<DeliveryShipmentAttempt>
    ): DomainResult<Unit> {
        if (attempt.projectId != shipment.projectId) {
            return DomainResult.Error(
                message = "Project mismatch: Attempt belongs to '${attempt.projectId}', but shipment is in '${shipment.projectId}'."
            )
        }
        if (attempt.shipmentId != shipment.shipmentId) {
            return DomainResult.Error(
                message = "Shipment ID mismatch: Attempt references '${attempt.shipmentId}', but parent shipment is '${shipment.shipmentId}'."
            )
        }
        if (shipment.currentStatus == DeliveryShipmentStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cannot record delivery attempt against CANCELLED shipment '${shipment.shipmentNo}'."
            )
        }
        if (shipment.currentStatus == DeliveryShipmentStatus.DRAFT || shipment.currentStatus == DeliveryShipmentStatus.READY) {
            return DomainResult.Error(
                message = "Cannot record delivery attempt on shipment that has not been dispatched yet (Current status: '${shipment.currentStatus.defaultLabel}')."
            )
        }
        if (attempt.attemptNo <= 0) {
            return DomainResult.Error(message = "Attempt number must be positive (>= 1).")
        }

        // Duplicate attempt number check
        if (existingAttempts.any { it.attemptNo == attempt.attemptNo }) {
            return DomainResult.Error(
                message = "Attempt number ${attempt.attemptNo} has already been recorded for shipment '${shipment.shipmentNo}'."
            )
        }

        val expectedNextNo = (existingAttempts.maxOfOrNull { it.attemptNo } ?: 0) + 1
        if (attempt.attemptNo != expectedNextNo) {
            return DomainResult.Error(
                message = "Sequential attempt number violation: Expected attempt #$expectedNextNo, but received #${attempt.attemptNo}."
            )
        }

        if (attempt.attemptedAt <= 0) {
            return DomainResult.Error(message = "Attempted at timestamp must be positive.")
        }

        return DomainResult.Success(Unit)
    }
}
