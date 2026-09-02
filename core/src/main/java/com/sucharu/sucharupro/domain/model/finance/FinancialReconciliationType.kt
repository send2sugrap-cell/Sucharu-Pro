package com.sucharu.sucharupro.domain.model.finance

/**
 * Types of financial reconciliations supported across financial subsystems (Module 09 Step 08).
 */
enum class FinancialReconciliationType(val defaultLabel: String) {
    LEDGER("General Ledger Reconciliation"),
    CASH("Cash in Hand Reconciliation"),
    BANK("Bank Statement Reconciliation"),
    CUSTOMER_RECEIVABLE("Customer Receivable Reconciliation"),
    VENDOR_PAYABLE("Vendor Payable Reconciliation"),
    EXPENSE("Expense & Cost Reconciliation"),
    PAYMENT("Customer Payment Reconciliation"),
    REFUND("Customer Refund Reconciliation"),
    ADJUSTMENT("Credit/Debit Adjustment Reconciliation"),
    PERIOD("Full Period Financial Reconciliation")
}
