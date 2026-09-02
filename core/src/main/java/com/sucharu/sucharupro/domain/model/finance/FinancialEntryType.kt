package com.sucharu.sucharupro.domain.model.finance

/**
 * Directional entry indicator for financial movements (Module 09 Step 01).
 */
enum class FinancialEntryType(val defaultLabel: String) {
    DEBIT("Debit (Dr)"),
    CREDIT("Credit (Cr)")
}
