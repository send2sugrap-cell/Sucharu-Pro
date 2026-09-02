package com.sucharu.sucharupro.domain.model.finance

/**
 * Accounting direction of a financial adjustment (Module 09 Step 07).
 * All monetary amounts remain positive; this enum explicitly defines whether the adjustment
 * acts as a DEBIT or CREDIT in double-entry / ledger contexts.
 */
enum class FinancialAdjustmentDirection(val defaultLabel: String) {
    DEBIT("Debit (+)"),
    CREDIT("Credit (-)")
}
