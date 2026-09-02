package com.sucharu.sucharupro.domain.model.finance

/**
 * Strict lifecycle states for financial transactions (Module 09 Step 01).
 */
enum class FinancialTransactionStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending Approval"),
    POSTED("Posted to Ledger"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == POSTED || this == REJECTED || this == CANCELLED
}
