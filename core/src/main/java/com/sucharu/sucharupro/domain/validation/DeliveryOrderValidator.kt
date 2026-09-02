package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus

/**
 * Domain validator for Delivery Orders (Module 08 Step 01).
 */
object DeliveryOrderValidator {

    /**
     * Validates structural invariants and cross-entity consistency for a [DeliveryOrder].
     */
    fun validateDeliveryOrder(order: DeliveryOrder, lines: List<DeliveryOrderLine>): DomainResult<Unit> {
        // Required field checks (Structural invariants)
        if (order.deliveryOrderId.isBlank()) return DomainResult.Error(message = "Delivery Order ID cannot be blank.")
        if (order.projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (order.deliveryOrderNo.isBlank()) return DomainResult.Error(message = "Delivery Order Number cannot be blank.")
        if (order.createdBy.isBlank()) return DomainResult.Error(message = "Created By cannot be blank.")
        if (order.createdAt <= 0) return DomainResult.Error(message = "Created At timestamp must be positive.")
        if (order.updatedAt < order.createdAt) {
            return DomainResult.Error(message = "Updated At cannot be before Created At.")
        }

        // Line consistency checks
        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Delivery Order must have at least one line item.")
        }

        for (line in lines) {
            if (line.projectId != order.projectId) {
                return DomainResult.Error(
                    message = "Project ID mismatch: Line '${line.lineId}' belongs to project '${line.projectId}', but order belongs to '${order.projectId}'."
                )
            }
            if (line.deliveryOrderId != order.deliveryOrderId) {
                return DomainResult.Error(
                    message = "Delivery Order ID mismatch: Line '${line.lineId}' references order '${line.deliveryOrderId}', but current order is '${order.deliveryOrderId}'."
                )
            }

            // Invoke line-level validation
            val lineValidation = DeliveryOrderLineValidator.validateLine(line)
            if (lineValidation is DomainResult.Error) return lineValidation
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that immutable identity fields have not been altered during an update.
     */
    fun validateImmutableIdentity(original: DeliveryOrder, updated: DeliveryOrder): DomainResult<Unit> {
        if (original.deliveryOrderId != updated.deliveryOrderId) {
            return DomainResult.Error(message = "Delivery Order ID is immutable and cannot be changed.")
        }
        if (original.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable and cannot be changed.")
        }
        if (original.deliveryOrderNo != updated.deliveryOrderNo) {
            return DomainResult.Error(message = "Delivery Order Number is immutable and cannot be changed.")
        }
        if (original.createdBy != updated.createdBy) {
            return DomainResult.Error(message = "Created By is immutable and cannot be changed.")
        }
        if (original.createdAt != updated.createdAt) {
            return DomainResult.Error(message = "Created At timestamp is immutable and cannot be changed.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates lifecycle state transitions for a Delivery Order.
     * Allowed flow: DRAFT -> PENDING -> APPROVED -> READY_FOR_DISPATCH -> DISPATCHED -> DELIVERED.
     * CANCELLED is allowed from DRAFT, PENDING, APPROVED, READY_FOR_DISPATCH.
     */
    fun validateStatusTransition(
        currentStatus: DeliveryOrderStatus,
        targetStatus: DeliveryOrderStatus
    ): DomainResult<Unit> {
        return when (currentStatus) {
            DeliveryOrderStatus.DRAFT -> {
                if (targetStatus == DeliveryOrderStatus.PENDING || targetStatus == DeliveryOrderStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Draft orders can only transition to Pending or Cancelled.")
                }
            }
            DeliveryOrderStatus.PENDING -> {
                if (targetStatus == DeliveryOrderStatus.APPROVED ||
                    targetStatus == DeliveryOrderStatus.CANCELLED ||
                    targetStatus == DeliveryOrderStatus.DRAFT) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Pending orders can transition to Approved, Cancelled, or back to Draft for revisions.")
                }
            }
            DeliveryOrderStatus.APPROVED -> {
                if (targetStatus == DeliveryOrderStatus.READY_FOR_DISPATCH || targetStatus == DeliveryOrderStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Approved orders can transition to Ready for Dispatch or Cancelled.")
                }
            }
            DeliveryOrderStatus.READY_FOR_DISPATCH -> {
                if (targetStatus == DeliveryOrderStatus.DISPATCHED || targetStatus == DeliveryOrderStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Ready for Dispatch orders can transition to Dispatched or Cancelled.")
                }
            }
            DeliveryOrderStatus.DISPATCHED -> {
                if (targetStatus == DeliveryOrderStatus.DELIVERED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Dispatched orders can only transition to Delivered.")
                }
            }
            DeliveryOrderStatus.DELIVERED -> {
                DomainResult.Error(message = "Delivered is a terminal status and cannot be changed.")
            }
            DeliveryOrderStatus.CANCELLED -> {
                DomainResult.Error(message = "Cancelled is a terminal status and cannot be changed.")
            }
        }
    }
}
