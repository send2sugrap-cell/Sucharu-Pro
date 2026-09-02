package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderStatusType

/**
 * Authoritative validator for the Order → Job Handoff commercial boundary.
 *
 * Validates commercial eligibility, item integrity, lifecycle constraints,
 * snapshot consistency, and duplicate handoff prevention.
 */
object OrderJobHandoffValidator {

    /**
     * Validates whether an [Order] is eligible to be handed off to production.
     *
     * @param order The commercial order to validate.
     * @param existingHandoffs Active handoffs for this order (to detect duplicate creation).
     */
    fun validateHandoffEligibility(
        order: Order,
        existingHandoffs: List<OrderJobHandoff> = emptyList()
    ): DomainResult<Unit> {
        // 1. Order identification
        if (order.orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        if (order.orderNumber.isBlank()) {
            return DomainResult.Error(message = "Order Number cannot be blank.")
        }
        if (order.customerId.isBlank()) {
            return DomainResult.Error(message = "Order must have a valid Customer reference.")
        }

        // 2. Lifecycle constraints
        if (order.status == OrderStatusType.CANCELLED) {
            return DomainResult.Error(message = "Cancelled orders cannot be handed off to production.")
        }
        if (order.status == OrderStatusType.DELIVERED) {
            return DomainResult.Error(message = "Delivered orders cannot be handed off to production.")
        }

        // 3. Commercial Line Items integrity
        if (order.items.isEmpty()) {
            return DomainResult.Error(message = "Order must contain at least one line item to initiate handoff.")
        }
        for (item in order.items) {
            if (item.itemId.isBlank()) {
                return DomainResult.Error(message = "Order item ID cannot be blank.")
            }
            if (item.description.isBlank()) {
                return DomainResult.Error(message = "Order item description cannot be blank.")
            }
            if (item.quantity <= 0) {
                return DomainResult.Error(message = "Item '${item.description}' quantity must be positive (was ${item.quantity}).")
            }
            if (item.unit.isBlank()) {
                return DomainResult.Error(message = "Item '${item.description}' unit cannot be blank.")
            }
            if (item.unitPrice.isNegative()) {
                return DomainResult.Error(message = "Item '${item.description}' unit price cannot be negative.")
            }
            if (item.discount.isNegative()) {
                return DomainResult.Error(message = "Item '${item.description}' discount cannot be negative.")
            }
            if (item.lineSubtotal.isNegative()) {
                return DomainResult.Error(message = "Item '${item.description}' line subtotal cannot be negative.")
            }
        }

        // 4. Financial totals
        if (order.totalAmount.isNegative()) {
            return DomainResult.Error(message = "Order commercial total amount cannot be negative.")
        }

        // 5. Duplicate active handoff prevention
        val hasActiveHandoff = existingHandoffs.any {
            it.orderId == order.orderId && it.handoffStatus != OrderJobHandoffStatus.CANCELLED
        }
        if (hasActiveHandoff) {
            return DomainResult.Error(
                message = "An active handoff record already exists for Order '${order.orderNumber}'."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates handoff status transitions.
     */
    fun validateStatusTransition(
        handoff: OrderJobHandoff,
        targetStatus: OrderJobHandoffStatus
    ): DomainResult<Unit> {
        val currentStatus = handoff.handoffStatus

        // Self transition rejection
        if (currentStatus == targetStatus) {
            return DomainResult.Error(
                message = "Handoff '${handoff.handoffId}' is already in ${currentStatus.defaultLabel} state."
            )
        }

        // Terminal state rejection
        if (currentStatus == OrderJobHandoffStatus.READY_FOR_PRODUCTION) {
            return DomainResult.Error(
                message = "Handoff is already READY FOR PRODUCTION (Terminal boundary state)."
            )
        }
        if (currentStatus == OrderJobHandoffStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cancelled handoff cannot undergo status changes (Terminal state)."
            )
        }

        // Valid transition path check
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Invalid handoff status transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}
