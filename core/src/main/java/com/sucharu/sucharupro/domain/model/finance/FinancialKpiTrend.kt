package com.sucharu.sucharupro.domain.model.finance

/**
 * Trend indicators for financial metrics (Module 09 Step 10).
 */
enum class FinancialKpiTrend(val defaultLabel: String) {
    UP("Trending Up"),
    DOWN("Trending Down"),
    STABLE("Stable"),
    IMPROVING("Improving"),
    DETERIORATING("Deteriorating"),
    INSUFFICIENT_DATA("Insufficient Historical Data")
}
