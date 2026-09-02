package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Expense Analytics, Spikes & Category intelligence (Module 09 Step 10).
 */
data class ExpenseAnalytics(
    val projectId: String,
    val totalPostedExpenses: Money,
    val approvedExpenses: Money,
    val pendingExpenses: Money,
    val categoryBreakdowns: List<ExpenseCategoryBreakdown> = emptyList(),
    val topExpenseCategories: List<ExpenseCategoryBreakdown> = emptyList(),
    val expenseToRevenueRatioPercent: Double? = null,
    val unusualExpenseSpikes: List<String> = emptyList(),
    val trend: FinancialKpiTrend = FinancialKpiTrend.STABLE,
    val analyzedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
