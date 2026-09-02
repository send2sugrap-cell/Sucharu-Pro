package com.sucharu.sucharupro.domain.model.finance

/**
 * Deterministic Financial Anomaly record (Module 09 Step 10).
 */
data class FinancialAnomaly(
    val anomalyId: String,
    val projectId: String,
    val type: FinancialAnomalyType,
    val severity: FinancialAnomalySeverity,
    val title: String,
    val description: String,
    val entityReferenceId: String? = null,
    val detectedAt: Long = System.currentTimeMillis()
) {
    init {
        require(anomalyId.isNotBlank()) { "Anomaly ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
    }
}

enum class FinancialAnomalyType(val defaultLabel: String) {
    DUPLICATE_LIKE_ACTIVITY("Potential Duplicate Transaction"),
    EXPENSE_SPIKE("Unusual Expense Spike"),
    REFUND_SPIKE("Unusual Refund Spike"),
    LARGE_ADJUSTMENT("Abnormally Large Adjustment"),
    SUDDEN_RECEIVABLE_GROWTH("Sudden Spike in Receivables"),
    SUDDEN_PAYABLE_GROWTH("Sudden Spike in Payables"),
    COLLECTION_DROP("Sudden Drop in Collections"),
    RECONCILIATION_VARIANCE("Unbalanced Reconciliation Variance"),
    PERIOD_CLOSING_EXCEPTION("Period Closing Anomaly"),
    LEDGER_IMBALANCE("Ledger Imbalance Detected"),
    REPEATED_CANCELLED_TRANSACTIONS("High Frequency of Cancelled/Rejected Transactions")
}

enum class FinancialAnomalySeverity(val defaultLabel: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}
