package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.validation.FinancialTransactionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinancialTransactionValidationTest {

    @Test
    fun `valid financial transaction passes validation`() {
        val transaction = FinancialTransaction(
            transactionId = "TXN-001",
            projectId = "PRJ-01",
            transactionNo = "FTX-001",
            transactionType = FinancialTransactionType.SALE,
            transactionStatus = FinancialTransactionStatus.DRAFT,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("15000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            transactionDate = 1000L,
            description = "Custom catalog printing order",
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val result = FinancialTransactionValidator.validateTransaction(transaction, "PRJ-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `project mismatch returns error`() {
        val transaction = FinancialTransaction(
            transactionId = "TXN-001",
            projectId = "PRJ-A",
            transactionNo = "FTX-001",
            transactionType = FinancialTransactionType.SALE,
            transactionStatus = FinancialTransactionStatus.DRAFT,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            transactionDate = 1000L,
            description = "Flyers print",
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val result = FinancialTransactionValidator.validateTransaction(transaction, "PRJ-B")
        assertTrue(result is DomainResult.Error)
        assertEquals("Project ID mismatch. Expected 'PRJ-B' but got 'PRJ-A'.", (result as DomainResult.Error).message)
    }

    @Test
    fun `posted status without postedBy returns error`() {
        val transaction = FinancialTransaction(
            transactionId = "TXN-001",
            projectId = "PRJ-01",
            transactionNo = "FTX-001",
            transactionType = FinancialTransactionType.RECEIPT,
            transactionStatus = FinancialTransactionStatus.POSTED,
            entryType = FinancialEntryType.CREDIT,
            amount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            transactionDate = 1000L,
            description = "Payment received",
            postedBy = null,
            postedAt = 1200L,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1200L
        )

        val result = FinancialTransactionValidator.validateTransaction(transaction, "PRJ-01")
        assertTrue(result is DomainResult.Error)
        assertEquals("Posted transactions must have a valid postedBy user ID.", (result as DomainResult.Error).message)
    }
}
