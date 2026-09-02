package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorRejectionDomainTest {

    @Test
    fun testRejectionCreationAndProperties() {
        val rejection = VendorRejection(
            rejectionId = "vrj_01",
            projectId = "PRJ-01",
            vendorId = "VND-01",
            rejectionReference = "VRJ-2026-0001",
            rejectionReason = "Material thickness out of spec",
            rejectedQuantity = BigDecimal("50"),
            rejectedValue = Money(2500.0),
            status = VendorRejectionStatus.DRAFT,
            disposition = VendorRejectionDisposition.RETURN_TO_VENDOR,
            returnRequired = true,
            replacementRequired = true
        )

        assertEquals("vrj_01", rejection.rejectionId)
        assertEquals(VendorRejectionStatus.DRAFT, rejection.status)
        assertEquals(VendorRejectionDisposition.RETURN_TO_VENDOR, rejection.disposition)
        assertEquals(BigDecimal("50"), rejection.rejectedQuantity)
        assertEquals(Money(2500.0), rejection.rejectedValue)
        assertTrue(rejection.returnRequired)
        assertTrue(rejection.replacementRequired)
    }

    @Test
    fun testRejectionStateTransitions() {
        assertTrue(VendorRejectionStatus.DRAFT.canTransitionTo(VendorRejectionStatus.PENDING_VENDOR_RESPONSE))
        assertTrue(VendorRejectionStatus.DRAFT.canTransitionTo(VendorRejectionStatus.CANCELLED))
        assertTrue(VendorRejectionStatus.PENDING_VENDOR_RESPONSE.canTransitionTo(VendorRejectionStatus.ACCEPTED))
        assertTrue(VendorRejectionStatus.PENDING_VENDOR_RESPONSE.canTransitionTo(VendorRejectionStatus.DISPUTED))
        assertTrue(VendorRejectionStatus.ACCEPTED.canTransitionTo(VendorRejectionStatus.RESOLVED))
        assertTrue(VendorRejectionStatus.RESOLVED.canTransitionTo(VendorRejectionStatus.CLOSED))

        assertFalse(VendorRejectionStatus.CLOSED.canTransitionTo(VendorRejectionStatus.PENDING_VENDOR_RESPONSE))
        assertFalse(VendorRejectionStatus.CANCELLED.canTransitionTo(VendorRejectionStatus.RESOLVED))
    }
}
