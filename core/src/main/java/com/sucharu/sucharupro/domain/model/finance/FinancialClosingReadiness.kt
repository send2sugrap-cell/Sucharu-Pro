package com.sucharu.sucharupro.domain.model.finance

/**
 * Period closing readiness status (Module 09 Step 08).
 */
enum class FinancialClosingReadinessStatus(val defaultLabel: String) {
    READY("Ready to Close"),
    NOT_READY("Pending Prerequisites"),
    BLOCKED("Closing Blocked");

    val canProceedWithClosing: Boolean
        get() = this == READY
}

/**
 * Categorized blocker / reason preventing period closing (Module 09 Step 08).
 */
enum class FinancialClosingBlockerReason(val defaultLabel: String) {
    UNRECONCILED_LEDGER("General ledger is out of balance or unreconciled"),
    UNRECONCILED_RECEIVABLE("Customer receivables have unresolved variances"),
    UNRECONCILED_PAYABLE("Vendor payables have unresolved variances"),
    UNRECONCILED_PAYMENT("Customer payments have unverified entries"),
    UNRECONCILED_SUPPLIER_PAYMENT("Supplier payments have unverified entries"),
    UNRECONCILED_EXPENSE("Expenses have unverified postings"),
    UNRECONCILED_REFUND("Refunds have unverified postings"),
    UNRECONCILED_ADJUSTMENT("Adjustments have unverified entries"),
    CASH_MISMATCH("Physical cash does not match calculated ledger cash"),
    BANK_MISMATCH("Bank statement does not match reconciled balance"),
    CRITICAL_DISCREPANCY("Critical discrepancy requires resolution or administrative waiver"),
    PENDING_APPROVAL("One or more financial transactions or documents are awaiting approval"),
    ALREADY_CLOSED("Accounting period is already closed and locked"),
    INVALID_PERIOD("Accounting period is not found or has an invalid date range")
}

/**
 * Detailed assessment of period closing readiness (Module 09 Step 08).
 */
data class FinancialClosingReadiness(
    val periodId: String,
    val projectId: String,
    val status: FinancialClosingReadinessStatus,
    val checklistItems: List<FinancialClosingChecklistItem> = emptyList(),
    val blockerReasons: List<FinancialClosingBlockerReason> = emptyList(),
    val openDiscrepancyCount: Int = 0,
    val criticalDiscrepancyCount: Int = 0,
    val evaluatedAt: Long = System.currentTimeMillis()
)
