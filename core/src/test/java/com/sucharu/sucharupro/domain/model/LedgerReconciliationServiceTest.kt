package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.finance.LedgerReconciliationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerReconciliationServiceTest {

    @Test
    fun `reconcile reports clean when debits equal credits and no orphan records`() {
        val txn = FinancialTransaction(
            transactionId = "TXN-01",
            projectId = "PRJ-01",
            transactionNo = "FTX-001",
            transactionType = FinancialTransactionType.SALE,
            transactionStatus = FinancialTransactionStatus.POSTED,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(1000.0),
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            transactionDate = 1000L,
            description = "Sales Invoice",
            createdBy = "USER-01"
        )

        val entry1 = FinancialLedgerEntry(
            entryId = "LED-01",
            transactionId = "TXN-01",
            projectId = "PRJ-01",
            entryNo = "LED-001",
            entryType = FinancialEntryType.DEBIT,
            amount = Money(1000.0),
            currency = "BDT",
            accountHead = "ACCOUNTS_RECEIVABLE",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            entryDate = 1000L,
            narration = "Debit AR",
            createdBy = "USER-01"
        )

        val entry2 = FinancialLedgerEntry(
            entryId = "LED-02",
            transactionId = "TXN-01",
            projectId = "PRJ-01",
            entryNo = "LED-002",
            entryType = FinancialEntryType.CREDIT,
            amount = Money(1000.0),
            currency = "BDT",
            accountHead = "SALES_REVENUE",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            entryDate = 1000L,
            narration = "Credit Revenue",
            createdBy = "USER-01"
        )

        val report = LedgerReconciliationService.reconcile(
            projectId = "PRJ-01",
            periodId = "PER-01",
            transactions = listOf(txn),
            ledgerEntries = listOf(entry1, entry2)
        )

        assertTrue(report.isBalanced)
        assertEquals(Money(1000.0), report.totalDebitAmount)
        assertEquals(Money(1000.0), report.totalCreditAmount)
        assertTrue(report.orphanTransactions.isEmpty())
        assertTrue(report.orphanLedgerEntries.isEmpty())
    }

    @Test
    fun `reconcile detects unbalanced entries and project mismatch`() {
        val entry1 = FinancialLedgerEntry(
            entryId = "LED-01",
            transactionId = "TXN-01",
            projectId = "PRJ-01",
            entryNo = "LED-001",
            entryType = FinancialEntryType.DEBIT,
            amount = Money(1000.0),
            currency = "BDT",
            accountHead = "ACCOUNTS_RECEIVABLE",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            entryDate = 1000L,
            narration = "Debit AR",
            createdBy = "USER-01"
        )

        val entryCrossProject = FinancialLedgerEntry(
            entryId = "LED-02",
            transactionId = "TXN-01",
            projectId = "PRJ-02", // Cross-project mismatch!
            entryNo = "LED-002",
            entryType = FinancialEntryType.CREDIT,
            amount = Money(800.0), // Unbalanced amount!
            currency = "BDT",
            accountHead = "SALES_REVENUE",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            entryDate = 1000L,
            narration = "Credit Revenue",
            createdBy = "USER-01"
        )

        val report = LedgerReconciliationService.reconcile(
            projectId = "PRJ-01",
            periodId = "PER-01",
            transactions = emptyList(),
            ledgerEntries = listOf(entry1, entryCrossProject)
        )

        assertFalse(report.isBalanced)
        assertFalse(report.projectMismatches.isEmpty())
        assertFalse(report.orphanLedgerEntries.isEmpty())
    }
}
