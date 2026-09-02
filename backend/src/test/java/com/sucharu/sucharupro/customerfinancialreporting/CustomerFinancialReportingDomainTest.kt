package com.sucharu.sucharupro.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.model.customerledger.CustomerStatementSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialReportingDomainTest {

    @Test
    fun testStatementReportModel() {
        val summary = CustomerStatementSummary(
            customerId = "CUS-01",
            customerFinancialAccountId = "CFA-01",
            openingBalance = BigDecimal.ZERO,
            totalInvoiced = BigDecimal("100000.0000"),
            totalPaid = BigDecimal("60000.0000"),
            totalAdvances = BigDecimal.ZERO,
            totalAdjustmentsCredit = BigDecimal.ZERO,
            totalAdjustmentsDebit = BigDecimal.ZERO,
            totalRefunds = BigDecimal.ZERO,
            totalAllocated = BigDecimal("60000.0000"),
            currentReceivableBalance = BigDecimal("40000.0000"),
            availableCreditBalance = BigDecimal.ZERO,
            netBalance = BigDecimal("40000.0000")
        )

        val report = CustomerStatementReport(
            customerId = "CUS-01",
            customerCode = "CUS-001",
            customerDisplayName = "ACME Corp",
            accountNumber = "ACC-001",
            fromDate = 1000L,
            toDate = 2000L,
            openingBalance = BigDecimal.ZERO,
            totalInvoiced = BigDecimal("100000.0000"),
            totalPaid = BigDecimal("60000.0000"),
            totalCredits = BigDecimal.ZERO,
            totalAdjustments = BigDecimal.ZERO,
            totalRefunds = BigDecimal.ZERO,
            totalAllocated = BigDecimal("60000.0000"),
            currentReceivableBalance = BigDecimal("40000.0000"),
            availableCreditBalance = BigDecimal.ZERO,
            closingNetBalance = BigDecimal("40000.0000"),
            entries = emptyList(),
            summary = summary
        )

        assertEquals("CUS-01", report.customerId)
        assertEquals(BigDecimal("40000.0000"), report.closingNetBalance)
    }

    @Test
    fun testReportTypesAndFormatsEnumCompleteness() {
        assertTrue(CustomerFinancialReportType.values().contains(CustomerFinancialReportType.CUSTOMER_STATEMENT))
        assertTrue(CustomerFinancialReportType.values().contains(CustomerFinancialReportType.RECEIVABLE_AGING))
        assertTrue(CustomerFinancialReportType.values().contains(CustomerFinancialReportType.SETTLEMENT_REPORT))
        assertTrue(CustomerFinancialReportType.values().contains(CustomerFinancialReportType.FINANCIAL_SUMMARY))

        assertTrue(CustomerFinancialReportFormat.values().contains(CustomerFinancialReportFormat.JSON))
        assertTrue(CustomerFinancialReportFormat.values().contains(CustomerFinancialReportFormat.CSV))
        assertTrue(CustomerFinancialReportFormat.values().contains(CustomerFinancialReportFormat.PDF))
    }
}
