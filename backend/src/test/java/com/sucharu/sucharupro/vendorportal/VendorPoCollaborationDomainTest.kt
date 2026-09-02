package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test

class VendorPoCollaborationDomainTest {

    @Test
    fun testPoAcknowledgementCreationAndDefaultFields() {
        val ack = VendorPoAcknowledgement(
            acknowledgementId = "ack-1",
            purchaseOrderId = "po-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            actorId = "user-1",
            acknowledgementType = VendorPoAcknowledgementType.ACKNOWLEDGED,
            promisedDeliveryDate = 1750000000L,
            comment = "Order accepted and on track.",
            acknowledgedAt = 1740000000L
        )

        assertEquals("ack-1", ack.acknowledgementId)
        assertEquals(VendorPoAcknowledgementType.ACKNOWLEDGED, ack.acknowledgementType)
        assertEquals("Order accepted and on track.", ack.comment)
        assertNull(ack.exceptionDetails)
        assertNull(ack.declineReason)
    }

    @Test
    fun testPoAcknowledgementWithException() {
        val ack = VendorPoAcknowledgement(
            acknowledgementId = "ack-2",
            purchaseOrderId = "po-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            actorId = "user-1",
            acknowledgementType = VendorPoAcknowledgementType.ACKNOWLEDGED_WITH_EXCEPTION,
            exceptionDetails = "Delivery delayed by 2 days due to raw material transport.",
            promisedDeliveryDate = 1750200000L,
            acknowledgedAt = 1740000000L
        )

        assertEquals(VendorPoAcknowledgementType.ACKNOWLEDGED_WITH_EXCEPTION, ack.acknowledgementType)
        assertNotNull(ack.exceptionDetails)
        assertTrue(ack.exceptionDetails!!.contains("Delivery delayed"))
    }

    @Test
    fun testPoAcknowledgementDecline() {
        val ack = VendorPoAcknowledgement(
            acknowledgementId = "ack-3",
            purchaseOrderId = "po-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            actorId = "user-1",
            acknowledgementType = VendorPoAcknowledgementType.DECLINED,
            declineReason = "Capacity fully booked for requested delivery window.",
            acknowledgedAt = 1740000000L
        )

        assertEquals(VendorPoAcknowledgementType.DECLINED, ack.acknowledgementType)
        assertEquals("Capacity fully booked for requested delivery window.", ack.declineReason)
    }
}
