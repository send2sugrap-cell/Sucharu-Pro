package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorRfqDomainTest {

    @Test
    fun testRfqModelPropertiesAndDefaults() {
        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-2026-001",
            title = "Printing Paper Supply",
            requestedBy = "user-1",
            responseDeadline = System.currentTimeMillis() + 86400000L,
            createdBy = "user-1"
        )

        assertEquals("rfq-1", rfq.rfqId)
        assertEquals(VendorRfqStatus.DRAFT, rfq.status)
        assertTrue(rfq.status.isEditable)
        assertFalse(rfq.status.isTerminal)
        assertEquals("BDT", rfq.currency)
    }

    @Test
    fun testRfqItemModelProperties() {
        val item = VendorRfqItem(
            rfqItemId = "item-1",
            rfqId = "rfq-1",
            sequenceNumber = 1,
            description = "80 GSM A4 Paper Ream",
            quantity = BigDecimal("500.00"),
            targetUnitPrice = Money("450.00")
        )

        assertEquals("item-1", item.rfqItemId)
        assertEquals(BigDecimal("500.00"), item.quantity)
        assertEquals(Money("450.00"), item.targetUnitPrice)
    }
}
