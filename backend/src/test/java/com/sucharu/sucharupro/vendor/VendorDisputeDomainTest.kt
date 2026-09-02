package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorDisputeDomainTest {

    @Test
    fun testDisputeCreationAndProperties() {
        val dispute = VendorDispute(
            disputeId = "vds_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            disputeReference = "VDS-2026-0001",
            disputeType = VendorDisputeType.QUALITY,
            priority = VendorDisputePriority.HIGH,
            status = VendorDisputeStatus.OPEN,
            subject = "Defective ink batch affecting 5000 units",
            description = "UV varnish adhesion failure on carton packaging",
            disputedQuantity = BigDecimal("5000"),
            disputedAmount = Money(25000.0),
            raisedBy = "user-1"
        )

        assertEquals("vds_01", dispute.disputeId)
        assertEquals(VendorDisputeStatus.OPEN, dispute.status)
        assertEquals(VendorDisputeType.QUALITY, dispute.disputeType)
        assertEquals(VendorDisputePriority.HIGH, dispute.priority)
        assertEquals(BigDecimal("5000"), dispute.disputedQuantity)
        assertEquals(Money(25000.0), dispute.disputedAmount)
    }

    @Test
    fun testDisputeStateTransitions() {
        assertTrue(VendorDisputeStatus.OPEN.canTransitionTo(VendorDisputeStatus.UNDER_REVIEW))
        assertTrue(VendorDisputeStatus.OPEN.canTransitionTo(VendorDisputeStatus.ESCALATED))
        assertTrue(VendorDisputeStatus.OPEN.canTransitionTo(VendorDisputeStatus.CANCELLED))
        assertTrue(VendorDisputeStatus.UNDER_REVIEW.canTransitionTo(VendorDisputeStatus.AWAITING_VENDOR))
        assertTrue(VendorDisputeStatus.UNDER_REVIEW.canTransitionTo(VendorDisputeStatus.RESOLUTION_PROPOSED))
        assertTrue(VendorDisputeStatus.RESOLUTION_PROPOSED.canTransitionTo(VendorDisputeStatus.RESOLVED))
        assertTrue(VendorDisputeStatus.RESOLVED.canTransitionTo(VendorDisputeStatus.CLOSED))

        assertFalse(VendorDisputeStatus.CLOSED.canTransitionTo(VendorDisputeStatus.OPEN))
        assertFalse(VendorDisputeStatus.CANCELLED.canTransitionTo(VendorDisputeStatus.UNDER_REVIEW))
    }
}
