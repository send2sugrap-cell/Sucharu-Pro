package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit test suite for [OrderLifecycleValidator].
 */
class OrderLifecycleValidatorTest {

    private val validItem = OrderItem(
        itemId = "item-001",
        description = "Brochure Print",
        quantity = 500,
        unit = "Pcs",
        unitPrice = 10.toMoney()
    )

    private val validOrder = Order(
        orderId = "ord-001",
        orderNumber = "ORD-000001",
        customerId = "cus-001",
        quotationId = "qt-001",
        approvedQuotationRevisionId = "rev-001",
        status = OrderStatusType.PENDING,
        priority = OrderPriority.NORMAL,
        items = listOf(validItem),
        discount = Money.ZERO,
        paymentTerms = PaymentTerms.DEFAULT,
        deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
        jobHandoffStatus = JobHandoffStatus.NOT_READY,
        notes = "Sample Note",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Valid Status Transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test01_pending_allowedTransitions() {
        val pendingOrder = validOrder.copy(status = OrderStatusType.PENDING)

        // PENDING -> CONFIRMED
        assertTrue(OrderLifecycleValidator.validateStatusTransition(pendingOrder, OrderStatusType.CONFIRMED) is DomainResult.Success)
        // PENDING -> ON_HOLD
        assertTrue(OrderLifecycleValidator.validateStatusTransition(pendingOrder, OrderStatusType.ON_HOLD) is DomainResult.Success)
        // PENDING -> CANCELLED
        assertTrue(OrderLifecycleValidator.validateStatusTransition(pendingOrder, OrderStatusType.CANCELLED) is DomainResult.Success)
    }

    @Test
    fun test02_pending_rejectedTransitions() {
        val pendingOrder = validOrder.copy(status = OrderStatusType.PENDING)

        // PENDING -> DELIVERED (rejected)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(pendingOrder, OrderStatusType.DELIVERED) is DomainResult.Error)
        // PENDING -> READY (rejected)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(pendingOrder, OrderStatusType.READY) is DomainResult.Error)
        // PENDING -> IN_PRODUCTION (rejected)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(pendingOrder, OrderStatusType.IN_PRODUCTION) is DomainResult.Error)
        // PENDING -> PENDING (self transition rejected)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(pendingOrder, OrderStatusType.PENDING) is DomainResult.Error)
    }

    @Test
    fun test03_confirmed_allowedTransitions() {
        val confirmedOrder = validOrder.copy(status = OrderStatusType.CONFIRMED)

        // CONFIRMED -> ON_HOLD
        assertTrue(OrderLifecycleValidator.validateStatusTransition(confirmedOrder, OrderStatusType.ON_HOLD) is DomainResult.Success)
        // CONFIRMED -> IN_PRODUCTION
        assertTrue(OrderLifecycleValidator.validateStatusTransition(confirmedOrder, OrderStatusType.IN_PRODUCTION) is DomainResult.Success)
        // CONFIRMED -> CANCELLED
        assertTrue(OrderLifecycleValidator.validateStatusTransition(confirmedOrder, OrderStatusType.CANCELLED) is DomainResult.Success)
    }

    @Test
    fun test04_confirmed_rejectedTransitions() {
        val confirmedOrder = validOrder.copy(status = OrderStatusType.CONFIRMED)

        // CONFIRMED -> PENDING (rejected)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(confirmedOrder, OrderStatusType.PENDING) is DomainResult.Error)
        // CONFIRMED -> DELIVERED (rejected)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(confirmedOrder, OrderStatusType.DELIVERED) is DomainResult.Error)
        // CONFIRMED -> READY (rejected)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(confirmedOrder, OrderStatusType.READY) is DomainResult.Error)
        // CONFIRMED -> CONFIRMED (rejected)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(confirmedOrder, OrderStatusType.CONFIRMED) is DomainResult.Error)
    }

    @Test
    fun test05_onHold_allowedAndRejectedTransitions() {
        val holdOrder = validOrder.copy(status = OrderStatusType.ON_HOLD)

        // Allowed
        assertTrue(OrderLifecycleValidator.validateStatusTransition(holdOrder, OrderStatusType.CONFIRMED) is DomainResult.Success)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(holdOrder, OrderStatusType.CANCELLED) is DomainResult.Success)

        // Rejected
        assertTrue(OrderLifecycleValidator.validateStatusTransition(holdOrder, OrderStatusType.PENDING) is DomainResult.Error)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(holdOrder, OrderStatusType.DELIVERED) is DomainResult.Error)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(holdOrder, OrderStatusType.READY) is DomainResult.Error)
        assertTrue(OrderLifecycleValidator.validateStatusTransition(holdOrder, OrderStatusType.IN_PRODUCTION) is DomainResult.Error)
    }

    @Test
    fun test06_terminalStates_rejectAllTransitions() {
        val deliveredOrder = validOrder.copy(status = OrderStatusType.DELIVERED)
        val cancelledOrder = validOrder.copy(status = OrderStatusType.CANCELLED)

        OrderStatusType.entries.forEach { target ->
            assertTrue(OrderLifecycleValidator.validateStatusTransition(deliveredOrder, target) is DomainResult.Error)
            assertTrue(OrderLifecycleValidator.validateStatusTransition(cancelledOrder, target) is DomainResult.Error)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cancellation Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test07_cancellation_reasonValidation() {
        val order = validOrder.copy(status = OrderStatusType.CONFIRMED)

        // Blank reasons rejected
        assertTrue(OrderLifecycleValidator.validateCancellation(order, null) is DomainResult.Error)
        assertTrue(OrderLifecycleValidator.validateCancellation(order, "") is DomainResult.Error)
        assertTrue(OrderLifecycleValidator.validateCancellation(order, "   ") is DomainResult.Error)

        // Non-blank reason accepted
        assertTrue(OrderLifecycleValidator.validateCancellation(order, "Client changed design scope") is DomainResult.Success)
        assertTrue(OrderLifecycleValidator.validateCancellation(order, "গ্রাহক অর্ডার বাতিল করতে চেয়েছেন") is DomainResult.Success)
    }

    @Test
    fun test08_cancellation_terminalStateProtection() {
        val delivered = validOrder.copy(status = OrderStatusType.DELIVERED)
        val cancelled = validOrder.copy(status = OrderStatusType.CANCELLED)

        assertTrue(OrderLifecycleValidator.validateCancellation(delivered, "Valid Reason") is DomainResult.Error)
        assertTrue(OrderLifecycleValidator.validateCancellation(cancelled, "Valid Reason") is DomainResult.Error)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test09_priorityChange_allowedAndRejected() {
        val pending = validOrder.copy(status = OrderStatusType.PENDING)
        val confirmed = validOrder.copy(status = OrderStatusType.CONFIRMED)
        val hold = validOrder.copy(status = OrderStatusType.ON_HOLD)
        val delivered = validOrder.copy(status = OrderStatusType.DELIVERED)
        val cancelled = validOrder.copy(status = OrderStatusType.CANCELLED)

        assertTrue(OrderLifecycleValidator.validatePriorityChange(pending, OrderPriority.URGENT) is DomainResult.Success)
        assertTrue(OrderLifecycleValidator.validatePriorityChange(confirmed, OrderPriority.HIGH) is DomainResult.Success)
        assertTrue(OrderLifecycleValidator.validatePriorityChange(hold, OrderPriority.NORMAL) is DomainResult.Success)

        assertTrue(OrderLifecycleValidator.validatePriorityChange(delivered, OrderPriority.URGENT) is DomainResult.Error)
        assertTrue(OrderLifecycleValidator.validatePriorityChange(cancelled, OrderPriority.HIGH) is DomainResult.Error)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Job Handoff Readiness Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test10_jobHandoffReadiness_validation() {
        val validConfirmed = validOrder.copy(status = OrderStatusType.CONFIRMED)
        assertTrue(OrderLifecycleValidator.validateJobHandoffReadiness(validConfirmed) is DomainResult.Success)

        // Cancelled / Delivered rejected
        val cancelled = validConfirmed.copy(status = OrderStatusType.CANCELLED)
        val delivered = validConfirmed.copy(status = OrderStatusType.DELIVERED)
        assertTrue(OrderLifecycleValidator.validateJobHandoffReadiness(cancelled) is DomainResult.Error)
        assertTrue(OrderLifecycleValidator.validateJobHandoffReadiness(delivered) is DomainResult.Error)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Order Integrity Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test11_orderIntegrity_validOrderPasses() {
        assertTrue(OrderLifecycleValidator.validateOrderIntegrity(validOrder) is DomainResult.Success)
    }

    @Test
    fun test12_isTerminalAndIsMutableHelpers() {
        val pending = validOrder.copy(status = OrderStatusType.PENDING)
        val confirmed = validOrder.copy(status = OrderStatusType.CONFIRMED)
        val delivered = validOrder.copy(status = OrderStatusType.DELIVERED)
        val cancelled = validOrder.copy(status = OrderStatusType.CANCELLED)

        assertFalse(OrderLifecycleValidator.isTerminal(pending))
        assertFalse(OrderLifecycleValidator.isTerminal(confirmed))
        assertTrue(OrderLifecycleValidator.isTerminal(delivered))
        assertTrue(OrderLifecycleValidator.isTerminal(cancelled))

        assertTrue(OrderLifecycleValidator.isMutable(pending))
        assertTrue(OrderLifecycleValidator.isMutable(confirmed))
        assertFalse(OrderLifecycleValidator.isMutable(delivered))
        assertFalse(OrderLifecycleValidator.isMutable(cancelled))
    }
}
