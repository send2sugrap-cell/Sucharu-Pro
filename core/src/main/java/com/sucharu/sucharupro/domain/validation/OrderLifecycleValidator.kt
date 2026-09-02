package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType

/**
 * Authoritative, deterministic validator for Commercial Customer Order lifecycles and integrity.
 *
 * Enforces business rules, transition validity, terminal-state protection,
 * cancellation reasons, and commercial snapshot immutability.
 */
object OrderLifecycleValidator {

    /**
     * Checks whether an order is in a terminal lifecycle state (DELIVERED or CANCELLED).
     */
    fun isTerminal(status: OrderStatusType): Boolean {
        return status == OrderStatusType.DELIVERED || status == OrderStatusType.CANCELLED
    }

    /**
     * Checks whether an order is in a terminal lifecycle state.
     */
    fun isTerminal(order: Order): Boolean = isTerminal(order.status)

    /**
     * Checks whether an order is mutable for operational lifecycle operations.
     */
    fun isMutable(order: Order): Boolean = !isTerminal(order)

    /**
     * Validates whether an order can transition to [targetStatus].
     */
    fun validateStatusTransition(
        order: Order,
        targetStatus: OrderStatusType
    ): DomainResult<Unit> {
        val currentStatus = order.status

        // 1. Check self-transition
        if (currentStatus == targetStatus) {
            return DomainResult.Error(
                message = "Order '${order.orderNumber}' is already in ${currentStatus.defaultLabel} state."
            )
        }

        // 2. Check terminal state mutations
        if (currentStatus == OrderStatusType.DELIVERED) {
            return DomainResult.Error(
                message = "Delivered orders cannot undergo status changes (Terminal state)."
            )
        }
        if (currentStatus == OrderStatusType.CANCELLED) {
            return DomainResult.Error(
                message = "Cancelled orders cannot undergo status changes (Terminal state)."
            )
        }

        // 3. Check specific lifecycle transition
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Invalid order status transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }

        // 4. If transitioning to CONFIRMED, perform full confirmation validation
        if (targetStatus == OrderStatusType.CONFIRMED && currentStatus == OrderStatusType.PENDING) {
            return validateConfirmation(order)
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that an order in PENDING status satisfies all integrity rules before CONFIRMATION.
     */
    fun validateConfirmation(order: Order): DomainResult<Unit> {
        if (order.status != OrderStatusType.PENDING && order.status != OrderStatusType.ON_HOLD) {
            return DomainResult.Error(
                message = "Only PENDING or ON_HOLD orders can be confirmed (current status: ${order.status.defaultLabel})."
            )
        }

        val integrityResult = validateOrderIntegrity(order)
        if (integrityResult is DomainResult.Error) {
            return integrityResult
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that an order can be cancelled and that a non-blank cancellation reason is provided.
     */
    fun validateCancellation(order: Order, reason: String?): DomainResult<Unit> {
        if (order.status == OrderStatusType.CANCELLED) {
            return DomainResult.Error(message = "Order '${order.orderNumber}' is already cancelled.")
        }
        if (order.status == OrderStatusType.DELIVERED) {
            return DomainResult.Error(message = "Delivered orders cannot be cancelled (Terminal state).")
        }

        val trimmedReason = reason?.trim().orEmpty()
        if (trimmedReason.isBlank()) {
            return DomainResult.Error(message = "A valid, non-blank cancellation reason is mandatory.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether an order can be placed on hold.
     */
    fun validateHold(order: Order): DomainResult<Unit> {
        if (order.status == OrderStatusType.ON_HOLD) {
            return DomainResult.Error(message = "Order '${order.orderNumber}' is already ON HOLD.")
        }
        if (order.status == OrderStatusType.DELIVERED) {
            return DomainResult.Error(message = "Delivered orders cannot be put ON HOLD.")
        }
        if (order.status == OrderStatusType.CANCELLED) {
            return DomainResult.Error(message = "Cancelled orders cannot be put ON HOLD.")
        }
        if (order.status != OrderStatusType.PENDING && order.status != OrderStatusType.CONFIRMED && order.status != OrderStatusType.IN_PRODUCTION) {
            return DomainResult.Error(message = "Cannot put order on hold from status '${order.status.defaultLabel}'.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether an order can be resumed from hold.
     */
    fun validateResume(order: Order): DomainResult<Unit> {
        if (order.status != OrderStatusType.ON_HOLD) {
            return DomainResult.Error(message = "Only orders currently ON HOLD can be resumed (current: ${order.status.defaultLabel}).")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates priority update request for an order.
     */
    fun validatePriorityChange(order: Order, newPriority: OrderPriority): DomainResult<Unit> {
        if (order.status == OrderStatusType.DELIVERED) {
            return DomainResult.Error(message = "Cannot update priority of a delivered order (Terminal state).")
        }
        if (order.status == OrderStatusType.CANCELLED) {
            return DomainResult.Error(message = "Cannot update priority of a cancelled order (Terminal state).")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates job handoff readiness prerequisites.
     */
    fun validateJobHandoffReadiness(order: Order): DomainResult<Unit> {
        if (order.status == OrderStatusType.CANCELLED) {
            return DomainResult.Error(message = "Cancelled orders cannot be marked ready for job handoff.")
        }
        if (order.status == OrderStatusType.DELIVERED) {
            return DomainResult.Error(message = "Delivered orders cannot be marked ready for job handoff.")
        }
        if (order.customerId.isBlank()) {
            return DomainResult.Error(message = "Order must have a valid Customer ID.")
        }
        if (order.items.isEmpty()) {
            return DomainResult.Error(message = "Order must contain at least one line item to enter job handoff.")
        }
        if (order.items.any { it.quantity <= 0 }) {
            return DomainResult.Error(message = "All order items must have positive quantity.")
        }
        if (order.totalAmount.isNegative()) {
            return DomainResult.Error(message = "Order total amount cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates full commercial and mathematical integrity of an Order entity.
     */
    fun validateOrderIntegrity(order: Order): DomainResult<Unit> {
        if (order.orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        if (order.orderNumber.isBlank()) {
            return DomainResult.Error(message = "Order Number cannot be blank.")
        }
        if (order.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        if (order.items.isEmpty()) {
            return DomainResult.Error(message = "Order must contain at least one line item.")
        }
        for (item in order.items) {
            if (item.itemId.isBlank()) {
                return DomainResult.Error(message = "Order item ID cannot be blank.")
            }
            if (item.description.isBlank()) {
                return DomainResult.Error(message = "Order item description cannot be blank.")
            }
            if (item.quantity <= 0) {
                return DomainResult.Error(message = "Order item '${item.description}' quantity must be positive (was ${item.quantity}).")
            }
            if (item.unit.isBlank()) {
                return DomainResult.Error(message = "Order item '${item.description}' unit cannot be blank.")
            }
            if (item.unitPrice.isNegative()) {
                return DomainResult.Error(message = "Order item '${item.description}' unit price cannot be negative.")
            }
            if (item.discount.isNegative()) {
                return DomainResult.Error(message = "Order item '${item.description}' discount cannot be negative.")
            }
        }
        if (order.discount.isNegative()) {
            return DomainResult.Error(message = "Order-level discount cannot be negative.")
        }
        if (order.subtotal.isNegative()) {
            return DomainResult.Error(message = "Order subtotal cannot be negative.")
        }
        if (order.totalAmount.isNegative()) {
            return DomainResult.Error(message = "Order total amount cannot be negative.")
        }
        if (order.createdAt.isBlank()) {
            return DomainResult.Error(message = "Order creation timestamp cannot be blank.")
        }
        if (order.updatedAt.isBlank()) {
            return DomainResult.Error(message = "Order updated timestamp cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }
}
