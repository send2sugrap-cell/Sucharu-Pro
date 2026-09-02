package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.validation.CustomerReceivableValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableValidationTest {

    @Test
    fun `valid customer receivable passes validation`() {
        val receivable = CustomerReceivable(
            receivableId = "REC-001",
            receivableNo = "RCV-2026-001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-001",
            originalAmount = Money(BigDecimal("15000.00")),
            settledAmount = Money.ZERO,
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            status = CustomerReceivableStatus.OPEN,
            agingBucket = ReceivableAgingBucket.CURRENT,
            description = "Commercial invoice receivable",
            createdBy = "acct-1"
        )

        val result = CustomerReceivableValidator.validateReceivable(receivable, "PRJ-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero or negative original amount throws IllegalArgumentException on creation`() {
        CustomerReceivable(
            receivableId = "REC-001",
            receivableNo = "RCV-2026-001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-001",
            originalAmount = Money.ZERO,
            settledAmount = Money.ZERO,
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Invalid zero amount",
            createdBy = "acct-1"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `settled amount exceeding original amount throws IllegalArgumentException`() {
        CustomerReceivable(
            receivableId = "REC-001",
            receivableNo = "RCV-2026-001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-001",
            originalAmount = Money(BigDecimal("1000.00")),
            settledAmount = Money(BigDecimal("1500.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Excess settlement",
            createdBy = "acct-1"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid lowercase currency code throws IllegalArgumentException`() {
        CustomerReceivable(
            receivableId = "REC-001",
            receivableNo = "RCV-2026-001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-001",
            originalAmount = Money(BigDecimal("1000.00")),
            settledAmount = Money.ZERO,
            currency = "bdt",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Invalid currency",
            createdBy = "acct-1"
        )
    }

    @Test
    fun `cancelled receivable without reason fails validation`() {
        val receivable = CustomerReceivable(
            receivableId = "REC-001",
            receivableNo = "RCV-2026-001",
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-001",
            originalAmount = Money(BigDecimal("1000.00")),
            settledAmount = Money.ZERO,
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            status = CustomerReceivableStatus.CANCELLED,
            description = "Cancelled without reason",
            createdBy = "acct-1",
            cancellationReason = null,
            cancelledAt = System.currentTimeMillis()
        )

        val result = CustomerReceivableValidator.validateReceivable(receivable, "PRJ-01")
        assertTrue(result is DomainResult.Error)
        assertEquals("Cancelled receivables must include a cancellation reason.", (result as DomainResult.Error).message)
    }
}
