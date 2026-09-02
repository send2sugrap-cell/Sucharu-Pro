package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Early Warning Risk Indicator (Module 09 Step 10).
 */
data class FinancialRiskIndicator(
    val riskId: String,
    val projectId: String,
    val type: FinancialRiskType,
    val severity: FinancialRiskSeverity,
    val title: String,
    val description: String,
    val metricValue: String,
    val threshold: String? = null,
    val sourceReference: String? = null,
    val detectedAt: Long = System.currentTimeMillis()
) {
    init {
        require(riskId.isNotBlank()) { "Risk ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
    }
}

enum class FinancialRiskType(val defaultLabel: String) {
    OVERDUE_RECEIVABLE_GROWTH("Rapidly Increasing Overdue Receivables"),
    OVERDUE_PAYABLE_GROWTH("Rapidly Increasing Overdue Payables"),
    COLLECTION_RATE_DECLINE("Declining Collection Rate"),
    ABNORMAL_EXPENSE_INCREASE("Abnormal Expense Increase"),
    NEGATIVE_CASH_TREND("Negative Cash Trend"),
    CUSTOMER_CONCENTRATION("Excessive Customer Concentration"),
    SUPPLIER_CONCENTRATION("Excessive Supplier Concentration"),
    RECONCILIATION_DISCREPANCY("Unresolved Reconciliation Discrepancies"),
    FREQUENT_ADJUSTMENTS("Frequent Financial Adjustments"),
    UNUSUAL_REFUNDS("Unusual Refund Activity"),
    PERIOD_CLOSING_EXCEPTION("Period Closing Exception")
}

enum class FinancialRiskSeverity(val defaultLabel: String) {
    INFO("Informational"),
    LOW("Low Severity"),
    MEDIUM("Medium Severity"),
    HIGH("High Severity"),
    CRITICAL("Critical Risk")
}
