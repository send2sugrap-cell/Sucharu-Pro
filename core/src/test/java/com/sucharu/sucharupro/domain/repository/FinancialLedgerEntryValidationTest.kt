package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.validation.FinancialLedgerEntryValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinancialLedgerEntryValidationTest {

    @Test
    fun `valid financial ledger entry passes validation`() {
        val entry = FinancialLedgerEntry(
            entryId = "LED-001",
            transactionId = "TXN-001",
            projectId = "PRJ-01",
            entryNo = "LEN-001",
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            accountHead = "ACCOUNTS_RECEIVABLE",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            entryDate = 1000L,
            narration = "Order posted debit to receivables",
            createdBy = "acct-1",
            createdAt = 1000L
        )

        val result = FinancialLedgerEntryValidator.validateEntry(entry, "PRJ-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `ledger entry project mismatch returns error`() {
        val entry = FinancialLedgerEntry(
            entryId = "LED-001",
            transactionId = "TXN-001",
            projectId = "PRJ-A",
            entryNo = "LEN-001",
            entryType = FinancialEntryType.CREDIT,
            amount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            accountHead = "CASH_AND_BANK",
            referenceType = FinancialReferenceType.PAYMENT,
            referenceId = "PAY-001",
            entryDate = 1000L,
            narration = "Payment credit",
            createdBy = "acct-1",
            createdAt = 1000L
        )

        val result = FinancialLedgerEntryValidator.validateEntry(entry, "PRJ-B")
        assertTrue(result is DomainResult.Error)
        assertEquals("Project ID mismatch. Expected 'PRJ-B' but got 'PRJ-A'.", (result as DomainResult.Error).message)
    }
}
