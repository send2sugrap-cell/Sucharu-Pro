package com.sucharu.sucharupro.domain.model.finance

/**
 * State lifecycle of a financial adjustment or refund (Module 09 Step 07).
 */
enum class FinancialAdjustmentStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    POSTED("Posted to Ledger"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == POSTED || this == REJECTED || this == CANCELLED
}
