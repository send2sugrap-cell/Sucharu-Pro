package com.sucharu.sucharupro.domain.model.finance

/**
 * High-level business classification for financial transactions (Module 09 Step 01).
 */
enum class FinancialTransactionType(val defaultLabel: String) {
    SALE("Sale / Revenue"),
    RECEIPT("Customer Receipt"),
    PAYMENT("Vendor / Supplier Payment"),
    EXPENSE("Operating Expense"),
    REFUND("Customer Refund"),
    ADJUSTMENT("Financial Adjustment"),
    TRANSFER("Fund Transfer"),
    CREDIT("Credit Note"),
    DEBIT("Debit Note")
}
