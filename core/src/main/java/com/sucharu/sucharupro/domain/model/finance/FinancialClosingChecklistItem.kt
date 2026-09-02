package com.sucharu.sucharupro.domain.model.finance

/**
 * Codes representing mandatory verification checks required prior to period closing (Module 09 Step 08).
 */
enum class FinancialClosingChecklistCode(val defaultTitle: String, val isMandatory: Boolean = true) {
    LEDGER_BALANCED("General Ledger Debits equal Credits", true),
    NO_ORPHAN_TRANSACTIONS("No Orphan Financial Transactions", true),
    NO_ORPHAN_LEDGER_ENTRIES("No Orphan Financial Ledger Entries", true),
    RECEIVABLES_RECONCILED("Customer Receivables Reconciled", true),
    PAYABLES_RECONCILED("Vendor Payables Reconciled", true),
    CUSTOMER_PAYMENTS_RECONCILED("Customer Payments & Receipts Reconciled", true),
    SUPPLIER_PAYMENTS_RECONCILED("Supplier Payments & Settlements Reconciled", true),
    EXPENSES_RECONCILED("Operational Expenses Reconciled", true),
    REFUNDS_RECONCILED("Customer Refunds Reconciled", true),
    ADJUSTMENTS_RECONCILED("Credit & Debit Adjustments Reconciled", true),
    CASH_RECONCILED("Cash in Hand Physically Reconciled", true),
    BANK_RECONCILED("Bank Accounts Reconciled", true),
    NO_CRITICAL_DISCREPANCIES("No Unresolved Critical Discrepancies", true),
    NO_PENDING_APPROVALS("No Pending Period Approvals", true),
    AUDIT_EVENTS_PRESENT("Required Audit Trail Events Present", true),
    PERIOD_DATES_VALID("Period Date Range Valid", true),
    CLOSING_SNAPSHOT_READY("Closing Snapshot Ready for Generation", true)
}

/**
 * Individual checklist item state evaluated during closing review (Module 09 Step 08).
 */
data class FinancialClosingChecklistItem(
    val code: FinancialClosingChecklistCode,
    val title: String = code.defaultTitle,
    val isPassed: Boolean,
    val details: String = "",
    val verifiedAt: Long = System.currentTimeMillis()
)
