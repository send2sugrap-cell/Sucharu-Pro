package com.sucharu.sucharupro.domain.model.finance

/**
 * Top-level dashboard aggregation for Finance Analytics & Governance (Module 09 Step 10).
 */
data class FinanceAnalyticsDashboard(
    val projectId: String,
    val summary: FinanceAnalyticsSummary,
    val profitability: ProfitabilityAnalytics,
    val cashFlow: CashFlowAnalytics,
    val receivable: ReceivableAnalytics,
    val payable: PayableAnalytics,
    val expense: ExpenseAnalytics,
    val healthScore: FinancialHealthScore,
    val topRisks: List<FinancialRiskIndicator> = emptyList(),
    val recentAnomalies: List<FinancialAnomaly> = emptyList(),
    val governanceControls: List<AnalyticsControlResult> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
