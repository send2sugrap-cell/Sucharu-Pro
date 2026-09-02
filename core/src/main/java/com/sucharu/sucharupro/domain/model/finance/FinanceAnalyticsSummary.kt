package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * High-level consolidated executive analytics summary (Module 09 Step 10).
 */
data class FinanceAnalyticsSummary(
    val projectId: String,
    val filter: AnalyticsFilter,
    val totalRevenue: Money,
    val totalExpenses: Money,
    val netProfit: Money,
    val netProfitMarginPercent: Double? = null,
    val cashPosition: Money,
    val bankPosition: Money,
    val totalReceivables: Money,
    val totalPayables: Money,
    val overdueReceivables: Money,
    val overduePayables: Money,
    val totalCustomerCollections: Money,
    val totalSupplierPayments: Money,
    val netCashMovement: Money,
    val collectionRatePercent: Double? = null,
    val payableSettlementRatePercent: Double? = null,
    val expenseRatioPercent: Double? = null,
    val financialHealthScore: FinancialHealthScore,
    val activeRiskCount: Int,
    val anomalyCount: Int,
    val governanceStatus: FinancialGovernanceStatus,
    val generatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
