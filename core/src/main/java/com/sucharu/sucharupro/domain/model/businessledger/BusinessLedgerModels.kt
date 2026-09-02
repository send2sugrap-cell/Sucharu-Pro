package com.sucharu.sucharupro.domain.model.businessledger

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Source systems and transaction types originating business ledger postings (Module 15 Step 03).
 */
enum class BusinessLedgerSourceType {
    BUSINESS_EXPENSE,
    VENDOR_PAYABLE,
    VENDOR_PAYMENT,
    VENDOR_PAYABLE_ADJUSTMENT,
    BUSINESS_ADJUSTMENT
}

/**
 * Deterministic posting classification distinguishing recognition, payment/settlement, and reversal.
 */
enum class BusinessLedgerPostingType {
    EXPENSE_RECOGNITION,
    VENDOR_LIABILITY_RECOGNITION,
    VENDOR_PAYMENT,
    EXPENSE_PAYMENT,
    ADJUSTMENT,
    REVERSAL;

    val isRecognition: Boolean
        get() = this in setOf(EXPENSE_RECOGNITION, VENDOR_LIABILITY_RECOGNITION)

    val isPayment: Boolean
        get() = this in setOf(VENDOR_PAYMENT, EXPENSE_PAYMENT)

    val isReversal: Boolean
        get() = this == REVERSAL
}

/**
 * Minimum canonical business ledger account/category abstraction.
 */
enum class BusinessLedgerAccountCategory {
    OPERATING_EXPENSE,
    PRODUCTION_COST,
    VENDOR_COST,
    TRANSPORT_COST,
    LABOUR_COST,
    OFFICE_EXPENSE,
    UTILITY_EXPENSE,
    MARKETING_EXPENSE,
    OTHER_EXPENSE,
    VENDOR_LIABILITY,
    CASH,
    BANK,
    OTHER_PAYMENT_ACCOUNT;

    val isExpenseOrCost: Boolean
        get() = this in setOf(
            OPERATING_EXPENSE, PRODUCTION_COST, VENDOR_COST,
            TRANSPORT_COST, LABOUR_COST, OFFICE_EXPENSE,
            UTILITY_EXPENSE, MARKETING_EXPENSE, OTHER_EXPENSE
        )

    val isLiability: Boolean
        get() = this == VENDOR_LIABILITY

    val isAssetOrPayment: Boolean
        get() = this in setOf(CASH, BANK, OTHER_PAYMENT_ACCOUNT)
}

/**
 * Status lifecycle of a ledger posting.
 */
enum class BusinessLedgerEntryStatus {
    POSTED,
    REVERSED
}

/**
 * Canonical Business Financial Posting (Immutable Projection Layer).
 */
data class BusinessLedgerPosting(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val postingNumber: String,
    val postingType: BusinessLedgerPostingType,
    val sourceType: BusinessLedgerSourceType,
    val sourceId: String,
    val accountCategory: BusinessLedgerAccountCategory,
    val debitAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val creditAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val postingDate: Long = System.currentTimeMillis(),
    val effectiveDate: Long = System.currentTimeMillis(),
    val description: String,
    val reference: String? = null,
    val jobId: String? = null,
    val vendorId: String? = null,
    val expenseId: String? = null,
    val payableId: String? = null,
    val allocationId: String? = null,
    val reversalOfPostingId: String? = null,
    val isReversed: Boolean = false,
    val reversalReason: String? = null,
    val reversedBy: String? = null,
    val reversedAt: Long? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null,
    val checksum: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
) {
    val netMovement: BigDecimal
        get() = debitAmount.subtract(creditAmount).setScale(4, RoundingMode.HALF_UP)
}

/**
 * Analytical Cost Attribution Entity.
 * Costs can be attributed to specific jobs/projects without altering source financial amounts.
 */
data class BusinessCostAllocation(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val allocationNumber: String,
    val sourceType: BusinessLedgerSourceType,
    val sourceId: String,
    val ledgerPostingId: String? = null,
    val jobId: String,
    val vendorId: String? = null,
    val costCategory: BusinessLedgerAccountCategory,
    val allocatedAmount: BigDecimal,
    val currency: String = "BDT",
    val allocationDate: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val isReversed: Boolean = false,
    val reversalReason: String? = null,
    val reversedBy: String? = null,
    val reversedAt: Long? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Immutable append-only audit event for business ledger operations.
 */
data class BusinessLedgerAuditEvent(
    val eventId: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceType: BusinessLedgerSourceType? = null,
    val sourceId: String? = null,
    val postingId: String? = null,
    val allocationId: String? = null,
    val action: String,
    val previousState: String? = null,
    val newState: String? = null,
    val amount: BigDecimal? = null,
    val reason: String? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null,
    val checksum: String? = null,
    val metadataJson: String? = null
)

/**
 * Read-only Business Ledger Balance Summary.
 * Formula: Opening Balance + Total Debit - Total Credit = Closing Balance
 */
data class BusinessLedgerBalanceSummary(
    val tenantId: String,
    val projectId: String,
    val openingBalance: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalDebit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalCredit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val netMovement: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val closingBalance: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val asOfTimestamp: Long = System.currentTimeMillis()
)

/**
 * Period-based Ledger Report Summary.
 */
data class BusinessLedgerPeriodSummary(
    val tenantId: String,
    val projectId: String,
    val fromDate: Long,
    val toDate: Long,
    val openingBalance: BigDecimal,
    val totalDebit: BigDecimal,
    val totalCredit: BigDecimal,
    val netMovement: BigDecimal,
    val closingBalance: BigDecimal,
    val postingCount: Int,
    val currency: String = "BDT"
)

/**
 * Job cost allocation roll-up.
 */
data class BusinessJobCostSummary(
    val jobId: String,
    val totalAllocatedCost: BigDecimal,
    val allocationCount: Int,
    val currency: String = "BDT",
    val breakdownByCategory: Map<String, BigDecimal> = emptyMap(),
    val allocations: List<BusinessCostAllocation> = emptyList()
)

/**
 * Analytical model showing how much of a source expense/payable has been allocated vs remains unallocated.
 */
data class BusinessUnallocatedCostSummary(
    val sourceType: BusinessLedgerSourceType,
    val sourceId: String,
    val totalSourceAmount: BigDecimal,
    val allocatedAmount: BigDecimal,
    val unallocatedAmount: BigDecimal,
    val allocationPercentage: BigDecimal,
    val currency: String = "BDT"
)

/**
 * Aggregate summary of business cost allocations.
 */
data class BusinessCostAllocationSummary(
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val jobCount: Int,
    val currency: String = "BDT",
    val jobSummaries: List<BusinessJobCostSummary> = emptyList()
)
