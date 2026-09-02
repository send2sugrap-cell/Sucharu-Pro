package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorInvoiceDomainTest {

    @Test
    fun testInvoiceItemCalculations() {
        val item = VendorInvoiceItem(
            itemId = "vii_001",
            invoiceId = "vinv_001",
            purchaseOrderItemId = "poi_001",
            description = "Commercial Offset Paper 100gsm",
            quantity = BigDecimal("50.00"),
            unitPrice = Money(120.0),
            taxRate = BigDecimal("5.00"),
            taxAmount = Money(300.0),
            discountAmount = Money(100.0),
            lineTotal = Money(6200.0) // (50 * 120) + 300 - 100 = 6200
        )

        assertEquals("vii_001", item.itemId)
        assertEquals(BigDecimal("50.00"), item.quantity)
        assertEquals(Money(120.0), item.unitPrice)
        assertEquals(Money(6200.0), item.lineTotal)
    }

    @Test
    fun testInvoiceAggregateTotals() {
        val item1 = VendorInvoiceItem(
            itemId = "vii_001",
            invoiceId = "vinv_001",
            purchaseOrderItemId = "poi_001",
            description = "Item 1",
            quantity = BigDecimal("10"),
            unitPrice = Money(100.0),
            lineTotal = Money(1000.0)
        )
        val item2 = VendorInvoiceItem(
            itemId = "vii_002",
            invoiceId = "vinv_001",
            purchaseOrderItemId = "poi_002",
            description = "Item 2",
            quantity = BigDecimal("5"),
            unitPrice = Money(200.0),
            lineTotal = Money(1000.0)
        )

        val invoice = VendorInvoice(
            invoiceId = "vinv_001",
            projectId = "PRJ-01",
            vendorId = "VND-001",
            purchaseOrderId = "PO-001",
            invoiceNumber = "INV-2026-000001",
            vendorInvoiceNumber = "V-INV-9988",
            subtotal = Money(2000.0),
            taxAmount = Money(100.0),
            discountAmount = Money(50.0),
            shippingAmount = Money(150.0),
            otherCharges = Money(20.0),
            totalAmount = Money(2220.0), // 2000 + 100 - 50 + 150 + 20 = 2220
            items = listOf(item1, item2)
        )

        assertEquals(Money(2000.0), invoice.subtotal)
        assertEquals(Money(2220.0), invoice.totalAmount)
        assertEquals(2, invoice.items.size)
        assertEquals(VendorInvoiceStatus.DRAFT, invoice.status)
        assertEquals(VendorInvoiceMatchStatus.NOT_MATCHED, invoice.matchStatus)
    }

    @Test
    fun testStateMachineTransitions() {
        // DRAFT
        assertTrue(VendorInvoiceStatus.DRAFT.canTransitionTo(VendorInvoiceStatus.SUBMITTED))
        assertTrue(VendorInvoiceStatus.DRAFT.canTransitionTo(VendorInvoiceStatus.CANCELLED))
        assertFalse(VendorInvoiceStatus.DRAFT.canTransitionTo(VendorInvoiceStatus.APPROVED))

        // SUBMITTED
        assertTrue(VendorInvoiceStatus.SUBMITTED.canTransitionTo(VendorInvoiceStatus.MATCHED))
        assertTrue(VendorInvoiceStatus.SUBMITTED.canTransitionTo(VendorInvoiceStatus.UNDER_REVIEW))
        assertTrue(VendorInvoiceStatus.SUBMITTED.canTransitionTo(VendorInvoiceStatus.REJECTED))

        // MATCHED
        assertTrue(VendorInvoiceStatus.MATCHED.canTransitionTo(VendorInvoiceStatus.APPROVED))
        assertTrue(VendorInvoiceStatus.MATCHED.canTransitionTo(VendorInvoiceStatus.UNDER_REVIEW))

        // APPROVED
        assertTrue(VendorInvoiceStatus.APPROVED.canTransitionTo(VendorInvoiceStatus.POSTED))
        assertTrue(VendorInvoiceStatus.APPROVED.canTransitionTo(VendorInvoiceStatus.CANCELLED))

        // Terminal states
        assertTrue(VendorInvoiceStatus.POSTED.isTerminal)
        assertTrue(VendorInvoiceStatus.REJECTED.isTerminal)
        assertTrue(VendorInvoiceStatus.CANCELLED.isTerminal)

        assertFalse(VendorInvoiceStatus.POSTED.canTransitionTo(VendorInvoiceStatus.DRAFT))
        assertFalse(VendorInvoiceStatus.REJECTED.canTransitionTo(VendorInvoiceStatus.APPROVED))
        assertFalse(VendorInvoiceStatus.CANCELLED.canTransitionTo(VendorInvoiceStatus.MATCHED))
    }
}
