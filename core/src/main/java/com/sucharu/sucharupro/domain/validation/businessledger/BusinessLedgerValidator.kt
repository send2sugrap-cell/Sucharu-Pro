package com.sucharu.sucharupro.domain.validation.businessledger

import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

/**
 * Domain validator for Business Ledger and Cost Allocation rules (Module 15 Step 03).
 */
object BusinessLedgerValidator {

    private const val MAX_DECIMAL_SCALE = 4

    fun validatePrecision(amount: BigDecimal, fieldName: String = "Amount"): DomainResult<Unit> {
        if (amount.scale() > MAX_DECIMAL_SCALE) {
            return DomainResult.Error(
                message = "$fieldName precision cannot exceed $MAX_DECIMAL_SCALE decimal places (got scale ${amount.scale()})."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateCurrency(currency: String): DomainResult<Unit> {
        if (currency.isBlank() || currency.length != 3 || !currency.all { it.isLetter() }) {
            return DomainResult.Error(
                message = "Currency code must be a valid 3-letter ISO-4217 code, got '$currency'."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validatePosting(
        tenantId: String,
        projectId: String,
        postingType: BusinessLedgerPostingType,
        sourceType: BusinessLedgerSourceType,
        sourceId: String,
        accountCategory: BusinessLedgerAccountCategory,
        debitAmount: BigDecimal,
        creditAmount: BigDecimal,
        currency: String,
        createdBy: String,
        description: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (sourceId.isBlank()) {
            return DomainResult.Error(message = "Source ID cannot be blank.")
        }
        if (createdBy.isBlank()) {
            return DomainResult.Error(message = "CreatedBy actor ID cannot be blank.")
        }
        if (description.isBlank()) {
            return DomainResult.Error(message = "Posting description cannot be blank.")
        }

        val debitPrecRes = validatePrecision(debitAmount, "Debit amount")
        if (debitPrecRes is DomainResult.Error) return debitPrecRes

        val creditPrecRes = validatePrecision(creditAmount, "Credit amount")
        if (creditPrecRes is DomainResult.Error) return creditPrecRes

        if (debitAmount < BigDecimal.ZERO || creditAmount < BigDecimal.ZERO) {
            return DomainResult.Error(message = "Debit and credit amounts must be non-negative.")
        }

        val hasDebit = debitAmount > BigDecimal.ZERO
        val hasCredit = creditAmount > BigDecimal.ZERO

        if (!hasDebit && !hasCredit) {
            return DomainResult.Error(message = "Either debit amount or credit amount must be greater than zero.")
        }

        if (hasDebit && hasCredit) {
            return DomainResult.Error(message = "A single posting entry cannot have both positive debit and positive credit.")
        }

        val currRes = validateCurrency(currency)
        if (currRes is DomainResult.Error) return currRes

        return DomainResult.Success(Unit)
    }

    fun validateReversal(
        originalPosting: BusinessLedgerPosting,
        reversalReason: String,
        reversedBy: String
    ): DomainResult<Unit> {
        if (originalPosting.isReversed) {
            return DomainResult.Error(message = "Posting '${originalPosting.postingNumber}' is already reversed.")
        }
        if (originalPosting.postingType == BusinessLedgerPostingType.REVERSAL) {
            return DomainResult.Error(message = "Cannot reverse a posting that is already a compensating reversal.")
        }
        if (reversalReason.isBlank()) {
            return DomainResult.Error(message = "A mandatory reason must be provided when reversing a ledger posting.")
        }
        if (reversedBy.isBlank()) {
            return DomainResult.Error(message = "Actor performing the reversal cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateCostAllocation(
        tenantId: String,
        projectId: String,
        sourceType: BusinessLedgerSourceType,
        sourceId: String,
        jobId: String,
        allocatedAmount: BigDecimal,
        sourceTotalAmount: BigDecimal,
        existingAllocatedAmount: BigDecimal,
        currency: String,
        createdBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (sourceId.isBlank()) return DomainResult.Error(message = "Source ID cannot be blank.")
        if (jobId.isBlank()) return DomainResult.Error(message = "Job ID cannot be blank.")
        if (createdBy.isBlank()) return DomainResult.Error(message = "CreatedBy actor ID cannot be blank.")

        val precRes = validatePrecision(allocatedAmount, "Allocated amount")
        if (precRes is DomainResult.Error) return precRes

        if (allocatedAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Allocated amount must be strictly greater than zero.")
        }

        val currRes = validateCurrency(currency)
        if (currRes is DomainResult.Error) return currRes

        val availableToAllocate = sourceTotalAmount.subtract(existingAllocatedAmount)
        if (allocatedAmount > availableToAllocate) {
            return DomainResult.Error(
                message = "Allocation of $allocatedAmount exceeds remaining unallocated amount $availableToAllocate (Source total: $sourceTotalAmount, Previously allocated: $existingAllocatedAmount)."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validateAllocationReversal(
        allocation: BusinessCostAllocation,
        reversalReason: String,
        reversedBy: String
    ): DomainResult<Unit> {
        if (allocation.isReversed) {
            return DomainResult.Error(message = "Cost allocation '${allocation.allocationNumber}' is already reversed.")
        }
        if (reversalReason.isBlank()) {
            return DomainResult.Error(message = "A mandatory reason must be provided when reversing a cost allocation.")
        }
        if (reversedBy.isBlank()) {
            return DomainResult.Error(message = "Actor performing the reversal cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }
}
