package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Management forecast summary derived deterministically from historical trends (Module 09 Step 10).
 *
 * MANAGEMENT ESTIMATE ONLY. Not an accounting fact, never written to the financial ledger.
 */
data class FinancialForecastSummary(
    val projectId: String,
    val baselinePeriodLabel: String,
    val forecastPeriodLabel: String,
    val projectedRevenue: Money,
    val projectedExpenses: Money,
    val projectedNetProfit: Money,
    val projectedCashFlow: Money,
    val method: ForecastMethod = ForecastMethod.MOVING_AVERAGE,
    val confidenceLevel: String = "ESTIMATE",
    val generatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}

enum class ForecastMethod(val defaultLabel: String) {
    HISTORICAL_AVERAGE("Historical Average"),
    MOVING_AVERAGE("Weighted Moving Average"),
    LINEAR_EXTRAPOLATION("Linear Trend Extrapolation")
}
