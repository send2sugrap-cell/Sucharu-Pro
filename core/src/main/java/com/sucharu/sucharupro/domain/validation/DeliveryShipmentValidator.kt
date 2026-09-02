package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus

/**
 * Validates aggregate structure, dispatch eligibility, and immutability for Delivery Shipments (Module 08 Step 05).
 */
object DeliveryShipmentValidator {

    fun validateShipment(shipment: DeliveryShipment): DomainResult<Unit> {
        if (shipment.shipmentId.isBlank()) {
            return DomainResult.Error(message = "Shipment ID cannot be blank.")
        }
        if (shipment.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (shipment.shipmentNo.isBlank()) {
            return DomainResult.Error(message = "Shipment Number cannot be blank.")
        }
        if (shipment.deliveryOrderId.isBlank()) {
            return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        }
        if (shipment.deliveryChallanId.isBlank()) {
            return DomainResult.Error(message = "Delivery Challan ID cannot be blank.")
        }
        if (shipment.dispatchExecutionId.isBlank()) {
            return DomainResult.Error(message = "Dispatch Execution ID cannot be blank.")
        }
        if (shipment.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created By user ID cannot be blank.")
        }
        if (shipment.createdAt <= 0) {
            return DomainResult.Error(message = "Created At timestamp must be positive.")
        }
        if (shipment.updatedAt < shipment.createdAt) {
            return DomainResult.Error(message = "Updated At cannot precede Created At.")
        }
        if (shipment.trackingNumber != null && shipment.trackingNumber.isBlank()) {
            return DomainResult.Error(message = "Tracking number cannot be blank if specified.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateDispatchEligibility(
        dispatch: DispatchExecution,
        targetProjectId: String,
        deliveryOrderId: String,
        deliveryChallanId: String
    ): DomainResult<Unit> {
        if (dispatch.projectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project mismatch: Referenced dispatch execution belongs to '${dispatch.projectId}', not '$targetProjectId'."
            )
        }
        if (dispatch.deliveryOrderId != deliveryOrderId) {
            return DomainResult.Error(
                message = "Delivery Order mismatch: Dispatch references order '${dispatch.deliveryOrderId}', but shipment specifies '$deliveryOrderId'."
            )
        }
        if (dispatch.deliveryChallanId != deliveryChallanId) {
            return DomainResult.Error(
                message = "Delivery Challan mismatch: Dispatch references challan '${dispatch.deliveryChallanId}', but shipment specifies '$deliveryChallanId'."
            )
        }
        if (dispatch.status != DispatchExecutionStatus.DISPATCHED) {
            return DomainResult.Error(
                message = "Dispatch execution '${dispatch.dispatchNo}' is not eligible for shipment creation. Status must be DISPATCHED (current: '${dispatch.status}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateImmutableIdentity(
        original: DeliveryShipment,
        updated: DeliveryShipment
    ): DomainResult<Unit> {
        if (original.shipmentId != updated.shipmentId) {
            return DomainResult.Error(message = "Shipment ID is immutable and cannot be changed.")
        }
        if (original.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable and cannot be changed.")
        }
        if (original.shipmentNo != updated.shipmentNo) {
            return DomainResult.Error(message = "Shipment Number is immutable and cannot be changed.")
        }
        if (original.deliveryOrderId != updated.deliveryOrderId) {
            return DomainResult.Error(message = "Delivery Order ID is immutable and cannot be changed.")
        }
        if (original.deliveryChallanId != updated.deliveryChallanId) {
            return DomainResult.Error(message = "Delivery Challan ID is immutable and cannot be changed.")
        }
        if (original.dispatchExecutionId != updated.dispatchExecutionId) {
            return DomainResult.Error(message = "Dispatch Execution ID is immutable and cannot be changed.")
        }
        if (original.createdBy != updated.createdBy) {
            return DomainResult.Error(message = "Creator is immutable and cannot be changed.")
        }
        if (original.createdAt != updated.createdAt) {
            return DomainResult.Error(message = "Creation timestamp is immutable and cannot be changed.")
        }
        return DomainResult.Success(Unit)
    }
}
