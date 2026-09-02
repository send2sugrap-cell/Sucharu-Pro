package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPurchaseOrderDomainTest {

    @Test
    fun `test state machine valid transitions`() {
        val draft = VendorPurchaseOrderStatus.DRAFT
        assertTrue(draft.canTransitionTo(VendorPurchaseOrderStatus.PENDING_APPROVAL))
        assertTrue(draft.canTransitionTo(VendorPurchaseOrderStatus.CANCELLED))
        assertFalse(draft.canTransitionTo(VendorPurchaseOrderStatus.ISSUED))
        assertFalse(draft.canTransitionTo(VendorPurchaseOrderStatus.CLOSED))

        val pending = VendorPurchaseOrderStatus.PENDING_APPROVAL
        assertTrue(pending.canTransitionTo(VendorPurchaseOrderStatus.APPROVED))
        assertTrue(pending.canTransitionTo(VendorPurchaseOrderStatus.DRAFT))
        assertTrue(pending.canTransitionTo(VendorPurchaseOrderStatus.CANCELLED))
        assertFalse(pending.canTransitionTo(VendorPurchaseOrderStatus.CLOSED))

        val approved = VendorPurchaseOrderStatus.APPROVED
        assertTrue(approved.canTransitionTo(VendorPurchaseOrderStatus.ISSUED))
        assertTrue(approved.canTransitionTo(VendorPurchaseOrderStatus.CANCELLED))
        assertFalse(approved.canTransitionTo(VendorPurchaseOrderStatus.DRAFT))

        val issued = VendorPurchaseOrderStatus.ISSUED
        assertTrue(issued.canTransitionTo(VendorPurchaseOrderStatus.ACKNOWLEDGED))
        assertTrue(issued.canTransitionTo(VendorPurchaseOrderStatus.PARTIALLY_FULFILLED))
        assertTrue(issued.canTransitionTo(VendorPurchaseOrderStatus.CANCELLED))

        val ack = VendorPurchaseOrderStatus.ACKNOWLEDGED
        assertTrue(ack.canTransitionTo(VendorPurchaseOrderStatus.PARTIALLY_FULFILLED))
        assertTrue(ack.canTransitionTo(VendorPurchaseOrderStatus.FULFILLED))
        assertTrue(ack.canTransitionTo(VendorPurchaseOrderStatus.CANCELLED))

        val fulfilled = VendorPurchaseOrderStatus.FULFILLED
        assertTrue(fulfilled.canTransitionTo(VendorPurchaseOrderStatus.CLOSED))

        val closed = VendorPurchaseOrderStatus.CLOSED
        assertTrue(closed.isTerminal)
        assertFalse(closed.canTransitionTo(VendorPurchaseOrderStatus.DRAFT))

        val cancelled = VendorPurchaseOrderStatus.CANCELLED
        assertTrue(cancelled.isTerminal)
        assertFalse(cancelled.canTransitionTo(VendorPurchaseOrderStatus.APPROVED))
    }

    @Test
    fun `test line item mathematical calculations with Money`() {
        val item = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = "vpo_1",
            itemDescription = "Gloss Lamination Rolls",
            quantity = BigDecimal("50.00"),
            unitRate = Money(120.0),
            discount = Money(100.0),
            taxAmount = Money(300.0),
            lineTotal = Money(6200.0) // (50 * 120) + 300 - 100 = 6000 + 300 - 100 = 6200
        )

        val computed = (item.unitRate * item.quantity) + item.taxAmount - item.discount
        assertEquals(Money(6200.0), computed)
        assertEquals(Money(6200.0), item.lineTotal)
    }

    @Test
    fun `test order aggregate totals calculation`() {
        val item1 = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = "vpo_1",
            itemDescription = "Paper Board 300gsm",
            quantity = BigDecimal("10.00"),
            unitRate = Money(500.0),
            lineTotal = Money(5000.0)
        )
        val item2 = VendorPurchaseOrderItem(
            itemId = "poi_2",
            purchaseOrderId = "vpo_1",
            itemDescription = "UV Coating Liquid",
            quantity = BigDecimal("5.00"),
            unitRate = Money(800.0),
            lineTotal = Money(4000.0)
        )

        val subtotal = item1.lineTotal + item2.lineTotal
        val tax = Money(450.0)
        val discount = Money(250.0)
        val total = subtotal + tax - discount

        val order = VendorPurchaseOrder(
            purchaseOrderId = "vpo_1",
            projectId = "proj_1",
            orderNumber = "PO-2026-0001",
            vendorId = "v_1",
            requestedBy = "usr_creator",
            subtotal = subtotal,
            taxAmount = tax,
            discountAmount = discount,
            totalAmount = total,
            items = listOf(item1, item2)
        )

        assertEquals(Money(9000.0), order.subtotal)
        assertEquals(Money(9200.0), order.totalAmount)
        assertEquals(2, order.items.size)
        assertEquals(VendorPurchaseOrderStatus.DRAFT, order.status)
        assertTrue(order.status.isEditable)
    }
}
