package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businessledger.BusinessLedgerValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerDomainTest {

    @Test
    fun testPrecisionValidation() {
        val valid4Decimals = BigDecimal("100.1234")
        assertTrue(BusinessLedgerValidator.validatePrecision(valid4Decimals) is DomainResult.Success)

        val valid2Decimals = BigDecimal("100.50")
        assertTrue(BusinessLedgerValidator.validatePrecision(valid2Decimals) is DomainResult.Success)

        val invalid5Decimals = BigDecimal("100.12345")
        val res = BusinessLedgerValidator.validatePrecision(invalid5Decimals)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("precision cannot exceed 4"))
    }

    @Test
    fun testPostingValidationInvariants() {
        // Valid Debit posting
        val validDebit = BusinessLedgerValidator.validatePosting(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
            debitAmount = BigDecimal("5000.0000"),
            creditAmount = BigDecimal("0.0000"),
            currency = "BDT",
            createdBy = "USER-1",
            description = "Valid Expense Posting"
        )
        assertTrue(validDebit is DomainResult.Success)

        // Valid Credit posting
        val validCredit = BusinessLedgerValidator.validatePosting(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            postingType = BusinessLedgerPostingType.VENDOR_PAYMENT,
            sourceType = BusinessLedgerSourceType.VENDOR_PAYMENT,
            sourceId = "ALLOC-101",
            accountCategory = BusinessLedgerAccountCategory.CASH,
            debitAmount = BigDecimal("0.0000"),
            creditAmount = BigDecimal("3500.0000"),
            currency = "BDT",
            createdBy = "USER-1",
            description = "Valid Payment Posting"
        )
        assertTrue(validCredit is DomainResult.Success)

        // Invalid: Both debit and credit positive
        val bothPositive = BusinessLedgerValidator.validatePosting(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
            debitAmount = BigDecimal("5000.0000"),
            creditAmount = BigDecimal("2000.0000"),
            currency = "BDT",
            createdBy = "USER-1",
            description = "Ambiguous Posting"
        )
        assertTrue(bothPositive is DomainResult.Error)
        assertTrue((bothPositive as DomainResult.Error).message.contains("cannot have both positive debit and positive credit"))

        // Invalid: Both zero
        val bothZero = BusinessLedgerValidator.validatePosting(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
            debitAmount = BigDecimal("0.0000"),
            creditAmount = BigDecimal("0.0000"),
            currency = "BDT",
            createdBy = "USER-1",
            description = "Zero Posting"
        )
        assertTrue(bothZero is DomainResult.Error)
        assertTrue((bothZero as DomainResult.Error).message.contains("must be greater than zero"))

        // Invalid: Negative amount
        val negative = BusinessLedgerValidator.validatePosting(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
            debitAmount = BigDecimal("-500.0000"),
            creditAmount = BigDecimal("0.0000"),
            currency = "BDT",
            createdBy = "USER-1",
            description = "Negative Posting"
        )
        assertTrue(negative is DomainResult.Error)
        assertTrue((negative as DomainResult.Error).message.contains("must be non-negative"))
    }

    @Test
    fun testCurrencyValidation() {
        assertTrue(BusinessLedgerValidator.validateCurrency("BDT") is DomainResult.Success)
        assertTrue(BusinessLedgerValidator.validateCurrency("USD") is DomainResult.Success)
        assertTrue(BusinessLedgerValidator.validateCurrency("EUR") is DomainResult.Success)

        assertTrue(BusinessLedgerValidator.validateCurrency("BD") is DomainResult.Error)
        assertTrue(BusinessLedgerValidator.validateCurrency("123") is DomainResult.Error)
        assertTrue(BusinessLedgerValidator.validateCurrency("") is DomainResult.Error)
    }

    @Test
    fun testReversalValidation() {
        val originalPosting = BusinessLedgerPosting(
            id = "BLP-101",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            postingNumber = "POST-101",
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
            debitAmount = BigDecimal("1500.0000"),
            creditAmount = BigDecimal.ZERO,
            description = "Expense recognition for office supply",
            createdBy = "USER-1"
        )

        // Valid reversal
        val validRev = BusinessLedgerValidator.validateReversal(
            originalPosting = originalPosting,
            reversalReason = "Accidental duplicate entry",
            reversedBy = "USER-MGR-1"
        )
        assertTrue(validRev is DomainResult.Success)

        // Missing reason -> Error
        val blankReason = BusinessLedgerValidator.validateReversal(
            originalPosting = originalPosting,
            reversalReason = "",
            reversedBy = "USER-MGR-1"
        )
        assertTrue(blankReason is DomainResult.Error)
        assertTrue((blankReason as DomainResult.Error).message.contains("mandatory reason"))

        // Already reversed -> Error
        val alreadyReversed = originalPosting.copy(isReversed = true)
        val alreadyRevRes = BusinessLedgerValidator.validateReversal(
            originalPosting = alreadyReversed,
            reversalReason = "Second reversal attempt",
            reversedBy = "USER-MGR-1"
        )
        assertTrue(alreadyRevRes is DomainResult.Error)
        assertTrue((alreadyRevRes as DomainResult.Error).message.contains("already reversed"))

        // Reversal of a reversal -> Error
        val reversalPosting = originalPosting.copy(postingType = BusinessLedgerPostingType.REVERSAL)
        val revOfRevRes = BusinessLedgerValidator.validateReversal(
            originalPosting = reversalPosting,
            reversalReason = "Reversing reversal",
            reversedBy = "USER-MGR-1"
        )
        assertTrue(revOfRevRes is DomainResult.Error)
        assertTrue((revOfRevRes as DomainResult.Error).message.contains("already a compensating reversal"))
    }

    @Test
    fun testCostAllocationBounds() {
        val sourceTotal = BigDecimal("10000.0000")
        val existingAllocated = BigDecimal("7000.0000")

        // Valid allocation: 3000 <= 10000 - 7000
        val validAlloc = BusinessLedgerValidator.validateCostAllocation(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            jobId = "JOB-200",
            allocatedAmount = BigDecimal("3000.0000"),
            sourceTotalAmount = sourceTotal,
            existingAllocatedAmount = existingAllocated,
            currency = "BDT",
            createdBy = "USER-1"
        )
        assertTrue(validAlloc is DomainResult.Success)

        // Invalid: 3001 > remaining 3000
        val overAlloc = BusinessLedgerValidator.validateCostAllocation(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            jobId = "JOB-200",
            allocatedAmount = BigDecimal("3001.0000"),
            sourceTotalAmount = sourceTotal,
            existingAllocatedAmount = existingAllocated,
            currency = "BDT",
            createdBy = "USER-1"
        )
        assertTrue(overAlloc is DomainResult.Error)
        assertTrue((overAlloc as DomainResult.Error).message.contains("exceeds remaining unallocated amount"))
    }
}
