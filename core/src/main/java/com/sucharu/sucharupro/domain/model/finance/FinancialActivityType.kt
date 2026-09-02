package com.sucharu.sucharupro.domain.model.finance

/**
 * Event classification for financial audit and compliance tracking (Module 09 Step 01).
 */
enum class FinancialActivityType(val defaultLabel: String) {
    TRANSACTION_CREATED("Transaction Created"),
    TRANSACTION_UPDATED("Transaction Draft Updated"),
    TRANSACTION_SUBMITTED("Transaction Submitted for Approval"),
    TRANSACTION_POSTED("Transaction Posted to Ledger"),
    TRANSACTION_REJECTED("Transaction Rejected"),
    TRANSACTION_CANCELLED("Transaction Cancelled"),
    LEDGER_ENTRY_POSTED("Ledger Entry Posted")
}
