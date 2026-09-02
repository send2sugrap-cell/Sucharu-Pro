package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorDeliveryReceiptDomainTest {

    @Test
    fun testReceiptStatusTransitions() {
        // Valid transitions
        assertTrue(VendorDeliveryReceiptStatus.DRAFT.canTransitionTo(VendorDeliveryReceiptStatus.RECEIVING))
        assertTrue(VendorDeliveryReceiptStatus.DRAFT.canTransitionTo(VendorDeliveryReceiptStatus.CANCELLED))
        assertTrue(VendorDeliveryReceiptStatus.RECEIVING.canTransitionTo(VendorDeliveryReceiptStatus.RECEIVED))
        assertTrue(VendorDeliveryReceiptStatus.RECEIVED.canTransitionTo(VendorDeliveryReceiptStatus.INSPECTED))
        assertTrue(VendorDeliveryReceiptStatus.INSPECTED.canTransitionTo(VendorDeliveryReceiptStatus.ACCEPTED))
        assertTrue(VendorDeliveryReceiptStatus.INSPECTED.canTransitionTo(VendorDeliveryReceiptStatus.PARTIALLY_ACCEPTED))
        assertTrue(VendorDeliveryReceiptStatus.INSPECTED.canTransitionTo(VendorDeliveryReceiptStatus.REJECTED))

        // Terminal states cannot transition
        assertFalse(VendorDeliveryReceiptStatus.ACCEPTED.canTransitionTo(VendorDeliveryReceiptStatus.DRAFT))
        assertFalse(VendorDeliveryReceiptStatus.CANCELLED.canTransitionTo(VendorDeliveryReceiptStatus.RECEIVING))
        assertFalse(VendorDeliveryReceiptStatus.REJECTED.canTransitionTo(VendorDeliveryReceiptStatus.INSPECTED))
    }

    @Test
    fun testReceiptItemQuantitiesAndTotals() {
        val item = VendorDeliveryReceiptItem(
            receiptItemId = "vri_001",
            deliveryReceiptId = "vdr_001",
            purchaseOrderId = "po_001",
            purchaseOrderItemId = "poi_001",
            itemDescription = "Lamination Gloss 50 Micron",
            orderedQuantity = BigDecimal("1000.00"),
            previouslyReceivedQuantity = BigDecimal("200.00"),
            receivedQuantity = BigDecimal("400.00"),
            acceptedQuantity = BigDecimal("380.00"),
            rejectedQuantity = BigDecimal("10.00"),
            damagedQuantity = BigDecimal("10.00"),
            shortQuantity = BigDecimal.ZERO,
            unitRate = Money(BigDecimal("15.50")),
            lineTotal = Money(BigDecimal("6200.00"))
        )

        assertEquals("vri_001", item.receiptItemId)
        assertEquals(BigDecimal("1000.00"), item.orderedQuantity)
        assertEquals(BigDecimal("400.00"), item.receivedQuantity)
        assertEquals(BigDecimal("380.00"), item.acceptedQuantity)
        assertEquals(Money(BigDecimal("6200.00")), item.lineTotal)
    }

    @Test
    fun testReceiptAggregateCreation() {
        val receipt = VendorDeliveryReceipt(
            deliveryReceiptId = "vdr_001",
            projectId = "PRJ-01",
            tenantId = "TENANT-001",
            receiptNumber = "VDR-2026-0001",
            purchaseOrderId = "po_001",
            vendorId = "vendor_001",
            receivedBy = "user_001",
            status = VendorDeliveryReceiptStatus.DRAFT
        )

        assertEquals("vdr_001", receipt.deliveryReceiptId)
        assertEquals("PRJ-01", receipt.projectId)
        assertEquals(VendorDeliveryReceiptStatus.DRAFT, receipt.status)
        assertTrue(receipt.status.isEditable)
        assertFalse(receipt.status.isTerminal)
    }
}
