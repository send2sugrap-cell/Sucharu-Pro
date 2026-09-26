package com.sucharu.sucharupro.domain.service.finance

import com.sucharu.sucharupro.domain.model.finance.PaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.finance.ReconciliationHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancial360ServiceTest {

    private lateinit var service: CustomerFinancial360Service

    @Before
    fun setUp() {
        service = CustomerFinancial360Service()
    }

    @Test
    fun `buildCustomerFinancial360_calculatesBalancesAndPaymentAllocationsCorrectly`() {
        val summary = service.buildCustomerFinancial360("CUST-1001")

        assertNotNull(summary)
        assertEquals("CUST-1001", summary.customerId)
        assertEquals(BigDecimal("200000.00"), summary.totalInvoiced)
        assertEquals(BigDecimal("75000.00"), summary.totalCollected)
        assertEquals(BigDecimal("150000.00"), summary.totalOutstandingDue)
        assertEquals(BigDecimal("150000.00"), summary.overdueAmount)
        assertEquals(BigDecimal("25000.00"), summary.advanceCreditBalance)
        assertEquals(ReconciliationHealthStatus.RECONCILIATION_OK, summary.reconciliationStatus)

        // Verify Invoices & Payment Allocations
        assertEquals(2, summary.invoices.size)
        assertEquals(2, summary.payments.size)

        val unallocatedPayment = summary.payments.firstOrNull { it.allocationStatus == PaymentAllocationStatus.UNALLOCATED }
        assertNotNull(unallocatedPayment)
        assertEquals(BigDecimal("25000.00"), unallocatedPayment!!.unallocatedAmount)
    }
}
