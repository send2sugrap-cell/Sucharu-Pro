package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Cash Flow Analytics and Liquidity (Module 09 Step 10).
 */
data class CashFlowAnalytics(
    val projectId: String,
    val openingCash: Money,
    val cashInflows: Money,
    val cashOutflows: Money,
    val netCashMovement: Money,
    val closingCash: Money,
    val cashPosition: Money,
    val bankPosition: Money,
    val liquidityCoverageMonths: Double? = null,
    val trend: FinancialKpiTrend = FinancialKpiTrend.STABLE,
    val analyzedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
