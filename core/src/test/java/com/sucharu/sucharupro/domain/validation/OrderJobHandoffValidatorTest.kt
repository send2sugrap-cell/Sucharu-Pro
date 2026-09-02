package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffItem
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite for [OrderJobHandoffValidator].
 */
class OrderJobHandoffValidatorTest {

    private val sampleItem = OrderItem(
        itemId = "item-01",
        description = "ব্যানার প্রিন্টিং",
        specification = "১০x৩ ফিট",
        quantity = 5,
        unit = "Pcs",
        unitPrice = 500.toMoney(),
        discount = Money.ZERO
    )

    private val sampleOrder = Order(
        orderId = "ord-hnd-01",
        orderNumber = "ORD-2026-H01",
        customerId = "cus-001",
        quotationId = "qt-001",
        approvedQuotationRevisionId = "rev-001-v1",
        status = OrderStatusType.CONFIRMED,
        priority = OrderPriority.HIGH,
        items = listOf(sampleItem),
        discount = Money.ZERO,
        paymentTerms = PaymentTerms.DEFAULT,
        deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
        jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val sampleHandoff = OrderJobHandoff.fromOrder(
        handoffId = "hnd-001",
        order = sampleOrder,
        createdBy = "Admin",
        timestamp = "2026-08-16T10:30:00Z"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Eligibility Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test01_validConfirmedOrder_isEligible() {
        val result = OrderJobHandoffValidator.validateHandoffEligibility(sampleOrder)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun test02_cancelledOrder_isRejected() {
        val cancelled = sampleOrder.copy(status = OrderStatusType.CANCELLED)
        val result = OrderJobHandoffValidator.validateHandoffEligibility(cancelled)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cancelled"))
    }

    @Test
    fun test03_deliveredOrder_isRejected() {
        val delivered = sampleOrder.copy(status = OrderStatusType.DELIVERED)
        val result = OrderJobHandoffValidator.validateHandoffEligibility(delivered)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Delivered"))
    }

    @Test
    fun test04_duplicateActiveHandoff_isRejected() {
        val existingList = listOf(sampleHandoff)
        val result = OrderJobHandoffValidator.validateHandoffEligibility(sampleOrder, existingList)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun test05_cancelledPreviousHandoff_allowsNewHandoff() {
        val cancelledHandoff = sampleHandoff.copy(
            handoffStatus = OrderJobHandoffStatus.CANCELLED
        )
        val existingList = listOf(cancelledHandoff)
        val result = OrderJobHandoffValidator.validateHandoffEligibility(sampleOrder, existingList)
        assertTrue(result is DomainResult.Success)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handoff Status Transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test06_readyForHandoff_transitions() {
        // READY_FOR_HANDOFF -> HANDED_OFF (allowed)
        val toHandedOff = OrderJobHandoffValidator.validateStatusTransition(
            sampleHandoff,
            OrderJobHandoffStatus.HANDED_OFF
        )
        assertTrue(toHandedOff is DomainResult.Success)

        // READY_FOR_HANDOFF -> CANCELLED (allowed)
        val toCancelled = OrderJobHandoffValidator.validateStatusTransition(
            sampleHandoff,
            OrderJobHandoffStatus.CANCELLED
        )
        assertTrue(toCancelled is DomainResult.Success)

        // READY_FOR_HANDOFF -> READY_FOR_PRODUCTION (rejected jump)
        val toProd = OrderJobHandoffValidator.validateStatusTransition(
            sampleHandoff,
            OrderJobHandoffStatus.READY_FOR_PRODUCTION
        )
        assertTrue(toProd is DomainResult.Error)

        // Self-transition rejected
        val self = OrderJobHandoffValidator.validateStatusTransition(
            sampleHandoff,
            OrderJobHandoffStatus.READY_FOR_HANDOFF
        )
        assertTrue(self is DomainResult.Error)
    }

    @Test
    fun test07_handedOff_transitions() {
        val handedOff = sampleHandoff.copy(handoffStatus = OrderJobHandoffStatus.HANDED_OFF)

        // HANDED_OFF -> READY_FOR_PRODUCTION (allowed)
        val toProd = OrderJobHandoffValidator.validateStatusTransition(
            handedOff,
            OrderJobHandoffStatus.READY_FOR_PRODUCTION
        )
        assertTrue(toProd is DomainResult.Success)

        // HANDED_OFF -> CANCELLED (allowed)
        val toCancel = OrderJobHandoffValidator.validateStatusTransition(
            handedOff,
            OrderJobHandoffStatus.CANCELLED
        )
        assertTrue(toCancel is DomainResult.Success)

        // HANDED_OFF -> READY_FOR_HANDOFF (backwards rejected)
        val backwards = OrderJobHandoffValidator.validateStatusTransition(
            handedOff,
            OrderJobHandoffStatus.READY_FOR_HANDOFF
        )
        assertTrue(backwards is DomainResult.Error)
    }

    @Test
    fun test08_terminalStates_rejectAllTransitions() {
        val readyProd = sampleHandoff.copy(handoffStatus = OrderJobHandoffStatus.READY_FOR_PRODUCTION)
        val cancelled = sampleHandoff.copy(handoffStatus = OrderJobHandoffStatus.CANCELLED)

        OrderJobHandoffStatus.entries.forEach { target ->
            assertTrue(OrderJobHandoffValidator.validateStatusTransition(readyProd, target) is DomainResult.Error)
            assertTrue(OrderJobHandoffValidator.validateStatusTransition(cancelled, target) is DomainResult.Error)
        }
    }
}
