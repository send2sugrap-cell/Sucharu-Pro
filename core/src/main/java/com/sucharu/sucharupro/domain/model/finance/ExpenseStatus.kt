package com.sucharu.sucharupro.domain.model.finance

/**
 * Lifecycle status of an operational business expense (Module 09 Step 06).
 */
enum class ExpenseStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    POSTED("Posted to Ledger"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == POSTED || this == REJECTED || this == CANCELLED
}
