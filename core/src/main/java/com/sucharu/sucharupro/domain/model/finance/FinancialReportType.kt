package com.sucharu.sucharupro.domain.model.finance

/**
 * Canonical enumeration of all supported financial report types (Module 09 Step 09).
 *
 * Each value corresponds to a distinct reporting domain.
 * This enum must not be extended to cover non-financial reporting concerns.
 */
enum class FinancialReportType(val defaultLabel: String, val requiresFullAccess: Boolean = true) {
    DASHBOARD("Financial Dashboard", requiresFullAccess = false),
    PROFIT_AND_LOSS("Profit & Loss Statement"),
    BALANCE_SHEET("Balance Sheet"),
    CASH_FLOW("Cash Flow Summary"),
    TRIAL_BALANCE("Trial Balance"),
    GENERAL_LEDGER("General Ledger"),
    ACCOUNTS_RECEIVABLE("Accounts Receivable Report", requiresFullAccess = false),
    ACCOUNTS_PAYABLE("Accounts Payable Report", requiresFullAccess = false),
    EXPENSE_ANALYSIS("Expense Analysis"),
    CUSTOMER_PAYMENT("Customer Payment Report", requiresFullAccess = false),
    SUPPLIER_PAYMENT("Supplier Payment Report", requiresFullAccess = false),
    ADJUSTMENT("Financial Adjustment Report"),
    PERIOD_COMPARISON("Period Comparison Report"),
    KPI_SUMMARY("Financial KPI Summary")
}
