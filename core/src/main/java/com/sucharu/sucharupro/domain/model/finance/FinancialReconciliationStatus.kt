package com.sucharu.sucharupro.domain.model.finance

/**
 * Status of a financial reconciliation process (Module 09 Step 08).
 */
enum class FinancialReconciliationStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    IN_PROGRESS("In Progress"),
    MATCHED("Fully Matched / Balanced"),
    PARTIALLY_MATCHED("Partially Matched"),
    MISMATCHED("Discrepancy / Mismatched"),
    APPROVED("Approved"),
    CLOSED("Closed & Locked"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == CLOSED || this == CANCELLED
}
