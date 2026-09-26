package com.sucharu.sucharupro.domain.service.quotationlock

import com.sucharu.sucharupro.domain.model.quotationlock.CommercialLockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CommercialLockServiceTest {

    private lateinit var service: CommercialLockService

    @Before
    fun setUp() {
        service = CommercialLockService()
    }

    @Test
    fun `acceptAndLockQuotation_createsImmutableCommercialBaseline`() {
        val baseline = service.acceptAndLockQuotation(
            quoteId = "QUOTE-2026-001",
            quoteNumber = "QUOTE-001",
            versionNumber = 1,
            customerId = "CUST-1001",
            orderId = "ORD-1001",
            orderPriceSnapshotId = "SNAP-1001",
            agreedTotal = BigDecimal("410.00"),
            actorId = "CUST-1001"
        )

        assertNotNull(baseline)
        assertEquals("QUOTE-2026-001", baseline.quoteId)
        assertEquals("ORD-1001", baseline.orderId)
        assertEquals(1, baseline.acceptedVersionNumber)
        assertEquals(CommercialLockState.COMMERCIAL_LOCKED, baseline.lockState)
        assertEquals(BigDecimal("410.00"), baseline.agreedGrandTotal)

        // CRITICAL INVARIANT PROOF:
        // Commercial Lock Baseline MUST be explicitly immutable!
        assertTrue("Commercial Lock Baseline MUST be explicitly immutable!", baseline.isImmutable)
    }

    @Test
    fun `requestCommercialAmendment_createsAmendmentWithoutMutatingOriginalBaseline`() {
        val baseline = service.acceptAndLockQuotation(
            quoteId = "QUOTE-2026-001",
            orderId = "ORD-1001",
            agreedTotal = BigDecimal("410.00")
        )

        val amendment = service.requestCommercialAmendment(
            lockId = baseline.lockId,
            orderId = "ORD-1001",
            requestedBy = "STAFF-001",
            reason = "Customer requested additional 500 pcs quantity increase",
            changesJson = "{\"quantityChange\": 500}"
        )

        assertNotNull(amendment)
        assertEquals("ORD-1001", amendment.orderId)
        assertEquals(2, amendment.revisionNumber)
        assertEquals("PENDING", amendment.status)

        // CRITICAL INVARIANT PROOF:
        // Original Commercial Baseline grand total MUST REMAIN ৳410 (100% UNCHANGED)!
        val retrievedBaseline = service.getCommercialBaselineByOrder("ORD-1001")
        assertNotNull(retrievedBaseline)
        assertEquals(BigDecimal("410.00"), retrievedBaseline!!.agreedGrandTotal)
    }
}
