package com.sucharu.sucharupro.customerinvoice

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.validation.customerinvoice.CustomerInvoiceCalculator
import com.sucharu.sucharupro.domain.validation.customerinvoice.CustomerInvoiceValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 02: Customer Invoice Domain, Calculation Engine & Validator Tests.
 */
class CustomerInvoiceDomainTest {

    @Test
    fun testLineTotalCalculation() {
        val qty = BigDecimal("5.0000")
        val unitPrice = BigDecimal("150.0000")
        val discount = BigDecimal("50.0000")
        val tax = BigDecimal("20.0000")

        // 5 * 150 = 750 - 50 = 700 + 20 = 720
        val lineTotal = CustomerInvoiceCalculator.calculateLineTotal(qty, unitPrice, discount, tax)
        assertEquals(0, BigDecimal("720.0000").compareTo(lineTotal))
    }

    @Test
    fun testInvoiceTotalsCalculation() {
        val line1 = CustomerInvoiceLine(
            lineId = "L1", invoiceId = "INV-1", tenantId = "T1", projectId = "P1",
            description = "Flyer Printing", quantity = BigDecimal("10"), unitPrice = BigDecimal("100"),
            lineTotal = BigDecimal("1000.0000")
        )
        val line2 = CustomerInvoiceLine(
            lineId = "L2", invoiceId = "INV-1", tenantId = "T1", projectId = "P1",
            description = "Banner Printing", quantity = BigDecimal("2"), unitPrice = BigDecimal("500"),
            lineTotal = BigDecimal("1000.0000")
        )

        // Subtotal = 2000, Invoice Discount = 200, Tax = 100, Adjustment = 50 -> Grand Total = 1950
        val totals = CustomerInvoiceCalculator.calculateInvoiceTotals(
            lines = listOf(line1, line2),
            invoiceDiscount = BigDecimal("200.0000"),
            invoiceTax = BigDecimal("100.0000"),
            adjustment = BigDecimal("50.0000"),
            paidAmount = BigDecimal("500.0000")
        )

        assertEquals(0, BigDecimal("2000.0000").compareTo(totals.subtotal))
        assertEquals(0, BigDecimal("1950.0000").compareTo(totals.grandTotal))
        assertEquals(0, BigDecimal("1450.0000").compareTo(totals.dueAmount))
    }

    @Test
    fun testStatusTransitions() {
        // DRAFT -> ISSUED (Valid)
        assertTrue(CustomerInvoiceStatus.DRAFT.canTransitionTo(CustomerInvoiceStatus.ISSUED))
        // DRAFT -> CANCELLED (Valid)
        assertTrue(CustomerInvoiceStatus.DRAFT.canTransitionTo(CustomerInvoiceStatus.CANCELLED))

        // ISSUED -> PARTIALLY_PAID (Valid)
        assertTrue(CustomerInvoiceStatus.ISSUED.canTransitionTo(CustomerInvoiceStatus.PARTIALLY_PAID))
        // ISSUED -> PAID (Valid)
        assertTrue(CustomerInvoiceStatus.ISSUED.canTransitionTo(CustomerInvoiceStatus.PAID))
        // ISSUED -> VOID (Valid)
        assertTrue(CustomerInvoiceStatus.ISSUED.canTransitionTo(CustomerInvoiceStatus.VOID))

        // Terminal states cannot transition
        assertFalse(CustomerInvoiceStatus.CANCELLED.canTransitionTo(CustomerInvoiceStatus.ISSUED))
        assertFalse(CustomerInvoiceStatus.VOID.canTransitionTo(CustomerInvoiceStatus.PAID))
    }

    @Test
    fun testDraftValidationWithSuspendedAccountFails() {
        val account = CustomerFinancialAccount(
            financialAccountId = "CFA-001",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            customerId = "CUS-1",
            accountNumber = "ACC-001",
            status = CustomerFinancialAccountStatus.SUSPENDED
        )

        val line = CustomerInvoiceLine(
            lineId = "L1", invoiceId = "INV-1", tenantId = "TENANT-1", projectId = "PRJ-1",
            description = "Brochure Printing", quantity = BigDecimal("10"), unitPrice = BigDecimal("10")
        )

        val res = CustomerInvoiceValidator.validateDraftCreation(
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            customerId = "CUS-1",
            financialAccountId = "CFA-001",
            currency = "BDT",
            lines = listOf(line),
            account = account
        )
        assertTrue("Invoicing on suspended account must fail", res is DomainResult.Error)
    }

    @Test
    fun testCancellationWithoutReasonFails() {
        val invoice = CustomerInvoice(
            invoiceId = "INV-1",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            customerId = "CUS-1",
            customerFinancialAccountId = "CFA-1",
            invoiceNumber = "INV-001",
            status = CustomerInvoiceStatus.DRAFT
        )

        val res = CustomerInvoiceValidator.validateStatusTransition(
            invoice,
            CustomerInvoiceStatus.CANCELLED,
            reason = null
        )
        assertTrue("Cancelling without reason must fail", res is DomainResult.Error)
    }
}
