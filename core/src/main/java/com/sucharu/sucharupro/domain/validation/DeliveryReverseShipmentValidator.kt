package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipment
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipmentStatus

/**
 * Validation rules for Reverse Logistics Shipments (Module 08 Step 07).
 */
object DeliveryReverseShipmentValidator {

    fun validateShipment(shipment: DeliveryReturnShipment, targetProjectId: String): DomainResult<Unit> {
        if (shipment.reverseShipmentId.isBlank()) {
            return DomainResult.Error(message = "Reverse Shipment ID cannot be blank.")
        }
        if (shipment.returnId.isBlank()) {
            return DomainResult.Error(message = "Return ID cannot be blank.")
        }
        if (shipment.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (shipment.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Shipment belongs to '${shipment.projectId}', but target return is in '$targetProjectId'."
            )
        }
        if (shipment.carrierName.isBlank()) {
            return DomainResult.Error(message = "Carrier Name cannot be blank.")
        }
        if (shipment.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created By cannot be blank.")
        }
        if (shipment.createdAt <= 0) {
            return DomainResult.Error(message = "Creation timestamp must be positive.")
        }
        if (shipment.updatedAt < shipment.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot precede creation.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateTransition(
        currentStatus: DeliveryReturnShipmentStatus,
        targetStatus: DeliveryReturnShipmentStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition reverse shipment from terminal state '$currentStatus' to '$targetStatus'."
            )
        }

        val isValid = when (currentStatus) {
            DeliveryReturnShipmentStatus.DRAFT -> targetStatus in listOf(
                DeliveryReturnShipmentStatus.READY,
                DeliveryReturnShipmentStatus.CANCELLED
            )
            DeliveryReturnShipmentStatus.READY -> targetStatus in listOf(
                DeliveryReturnShipmentStatus.PICKUP_SCHEDULED,
                DeliveryReturnShipmentStatus.PICKED_UP,
                DeliveryReturnShipmentStatus.CANCELLED
            )
            DeliveryReturnShipmentStatus.PICKUP_SCHEDULED -> targetStatus in listOf(
                DeliveryReturnShipmentStatus.PICKED_UP,
                DeliveryReturnShipmentStatus.CANCELLED
            )
            DeliveryReturnShipmentStatus.PICKED_UP -> targetStatus in listOf(
                DeliveryReturnShipmentStatus.IN_TRANSIT,
                DeliveryReturnShipmentStatus.DELIVERED_TO_WAREHOUSE
            )
            DeliveryReturnShipmentStatus.IN_TRANSIT -> targetStatus in listOf(
                DeliveryReturnShipmentStatus.DELIVERED_TO_WAREHOUSE
            )
            DeliveryReturnShipmentStatus.DELIVERED_TO_WAREHOUSE,
            DeliveryReturnShipmentStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal reverse shipment transition from '$currentStatus' to '$targetStatus'."
            )
        }
    }
}
