package com.sucharu.sucharupro.domain.service.compliance

import com.sucharu.sucharupro.domain.model.compliance.AccountingPeriodStatus
import com.sucharu.sucharupro.domain.model.compliance.TaxRuleConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class TaxComplianceServiceTest {

    private lateinit var service: TaxComplianceService

    @Before
    fun setUp() {
        service = TaxComplianceService()
    }

    @Test
    fun `createInvoiceTaxSnapshot_computesTaxFromConfigurableRateAndPreservesImmutability`() = runBlocking {
        // 1. Create Configurable Tax Rule (Rate = 15.00%)
        val customRule = TaxRuleConfiguration(
            taxRuleId = "TAX-CUSTOM-15",
            taxCode = "VAT_CONFIGURABLE_15",
            taxName = "Configurable Printing VAT",
            ratePercentage = BigDecimal("15.00"),
            isActive = true,
            createdAt = "2026-09-26T20:30:00Z",
            updatedAt = "2026-09-26T20:30:00Z",
            createdBy = "ADMIN-001"
        )
        service.createTaxRule(customRule)

        // 2. Create Invoice Tax Snapshot for Invoice INV-2026-001 (Subtotal = ৳1000.00)
        val snapshot = service.createInvoiceTaxSnapshot(
            invoiceId = "INV-2026-001",
            subtotalAmount = BigDecimal("1000.00"),
            taxCode = "VAT_CONFIGURABLE_15",
            customerBin = "BIN-123456789",
            customerTin = "TIN-987654321"
        )

        assertNotNull(snapshot)
        assertEquals("INV-2026-001", snapshot.invoiceId)
        assertEquals(BigDecimal("150.00"), snapshot.taxAmount) // 15% of 1000 = 150
        assertEquals("BIN-123456789", snapshot.customerBin)

        // CRITICAL INVARIANT PROOF:
        // Invoice Tax Snapshot MUST be explicitly immutable!
        assertTrue("Invoice Tax Snapshot MUST be explicitly immutable!", snapshot.isImmutable)
    }

    @Test
    fun `closeAccountingPeriod_transitionsPeriodToClosedStatus`() = runBlocking {
        val closedPeriod = service.closeAccountingPeriod("PER-2026-08", "ACCOUNTS_HEAD")

        assertNotNull(closedPeriod)
        assertEquals("PER-2026-08", closedPeriod.periodId)
        assertEquals(AccountingPeriodStatus.CLOSED, closedPeriod.status)
    }
}
