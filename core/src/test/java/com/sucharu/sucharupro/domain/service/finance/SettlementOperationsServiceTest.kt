package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.finance.ReconciliationHealthStatus
import com.sucharu.sucharupro.domain.model.finance.SettlementOperationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SettlementOperationsServiceTest {

    private lateinit var service: SettlementOperationsService

    @Before
    fun setUp() {
        service = SettlementOperationsService()
    }

    @Test
    fun `buildSettlementOperationsSummary_computesUnallocatedTotalsAndReconciliationExceptions`() {
        val summary = service.buildSettlementOperationsSummary()

        assertNotNull(summary)
        assertEquals(1, summary.totalUnallocatedPaymentsCount)
        assertEquals(1, summary.totalPartiallyAllocatedCount)
        assertEquals(BigDecimal("65000.00"), summary.totalUnallocatedAmount)
        assertEquals(1, summary.reconciliationExceptionCount)

        val unallocatedItems = service.filterExceptionsByCategory(summary, SettlementOperationCategory.UNALLOCATED)
        assertEquals(1, unallocatedItems.size)
        assertEquals("PAY-2026-002", unallocatedItems.first().paymentId)

        val reconException = summary.reconciliationExceptions.first()
        assertEquals("ACC-CUST-002", reconException.accountId)
        assertEquals(ReconciliationHealthStatus.RECONCILIATION_EXCEPTION, reconException.healthStatus)
        assertEquals(BigDecimal("5000.00"), reconException.discrepancyAmount)
    }
}
