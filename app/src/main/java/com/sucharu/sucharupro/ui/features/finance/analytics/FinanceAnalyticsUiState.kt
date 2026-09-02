package com.sucharu.sucharupro.ui.features.finance.analytics

import com.sucharu.sucharupro.domain.model.finance.*

/**
 * UI State for Finance Analytics & Governance dashboard (Module 09 Step 10).
 */
data class FinanceAnalyticsUiState(
    val filter: AnalyticsFilter = AnalyticsFilter(projectId = "PRJ-DEFAULT"),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTab: FinanceAnalyticsTab = FinanceAnalyticsTab.OVERVIEW,
    val dashboard: FinanceAnalyticsDashboard? = null,
    val summary: FinanceAnalyticsSummary? = null,
    val profitability: ProfitabilityAnalytics? = null,
    val cashFlow: CashFlowAnalytics? = null,
    val receivable: ReceivableAnalytics? = null,
    val payable: PayableAnalytics? = null,
    val expense: ExpenseAnalytics? = null,
    val collectionPerformance: CollectionPerformanceAnalytics? = null,
    val supplierPayment: SupplierPaymentAnalytics? = null,
    val healthScore: FinancialHealthScore? = null,
    val risks: List<FinancialRiskIndicator> = emptyList(),
    val anomalies: List<FinancialAnomaly> = emptyList(),
    val governanceControls: List<AnalyticsControlResult> = emptyList(),
    val periodComparison: FinancialPeriodComparison? = null,
    val forecast: FinancialForecastSummary? = null,
    val snapshots: List<FinancialAnalyticsSnapshot> = emptyList(),
    val activityEvents: List<FinanceGovernanceActivityEvent> = emptyList()
)

enum class FinanceAnalyticsTab(val defaultLabel: String) {
    OVERVIEW("Overview"),
    HEALTH("Health Score"),
    PROFITABILITY("Profitability"),
    CASH_FLOW("Cash Flow"),
    RECEIVABLES("Receivables"),
    PAYABLES("Payables"),
    EXPENSES("Expenses"),
    COLLECTIONS("Collections"),
    RISKS("Risks"),
    ANOMALIES("Anomalies"),
    GOVERNANCE("Governance"),
    COMPARISON("Comparison"),
    FORECAST("Forecast"),
    SNAPSHOTS("Snapshots")
}
