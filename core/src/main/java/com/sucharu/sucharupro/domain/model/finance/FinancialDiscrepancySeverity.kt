package com.sucharu.sucharupro.domain.model.finance

/**
 * Severity level of a financial reconciliation discrepancy (Module 09 Step 08).
 */
enum class FinancialDiscrepancySeverity(val defaultLabel: String) {
    LOW("Low - Informational"),
    MEDIUM("Medium - Minor Variance"),
    HIGH("High - Material Difference"),
    CRITICAL("Critical - Blocks Period Closing");

    val blocksClosing: Boolean
        get() = this == CRITICAL
}
