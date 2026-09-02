package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Profitability Analytics and Margins (Module 09 Step 10).
 */
data class ProfitabilityAnalytics(
    val projectId: String,
    val totalRevenue: Money,
    val totalExpenses: Money,
    val netProfit: Money,
    val grossProfit: Money = Money.ZERO,
    val netProfitMarginPercent: Double? = null,
    val expenseToRevenueRatioPercent: Double? = null,
    val trend: FinancialKpiTrend = FinancialKpiTrend.STABLE,
    val status: ProfitabilityStatus = ProfitabilityStatus.PROFITABLE,
    val analyzedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}

enum class ProfitabilityStatus(val defaultLabel: String) {
    PROFITABLE("Profitable"),
    BREAK_EVEN("Break Even"),
    LOW_MARGIN("Low Margin"),
    LOSS("Loss / Deficit"),
    INSUFFICIENT_DATA("Insufficient Data")
}
