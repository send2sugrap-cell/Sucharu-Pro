package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Period-over-period comparison metrics for management analytics (Module 09 Step 10).
 */
data class FinancialPeriodComparison(
    val projectId: String,
    val periodALabel: String,
    val periodBLabel: String,
    val revenueA: Money,
    val revenueB: Money,
    val revenueVariance: Money,
    val revenueVariancePercent: Double? = null,
    val expensesA: Money,
    val expensesB: Money,
    val expensesVariance: Money,
    val expensesVariancePercent: Double? = null,
    val netProfitA: Money,
    val netProfitB: Money,
    val netProfitVariance: Money,
    val netProfitVariancePercent: Double? = null,
    val cashInA: Money,
    val cashInB: Money,
    val cashInVariance: Money,
    val cashInVariancePercent: Double? = null,
    val comparedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
