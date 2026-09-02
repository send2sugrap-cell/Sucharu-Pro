package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

data class ExpenseCategoryBreakdown(
    val categoryId: String,
    val categoryName: String,
    val totalAmount: Money,
    val expenseCount: Int,
    val percentageOfTotal: Double = 0.0
)

data class ExpenseSummary(
    val projectId: String,
    val totalExpenses: Money,
    val postedExpenses: Money,
    val pendingExpenses: Money,
    val approvedExpenses: Money,
    val draftExpenses: Money,
    val cancelledExpenses: Money,
    val totalCount: Int,
    val postedCount: Int,
    val pendingCount: Int,
    val categoryBreakdowns: List<ExpenseCategoryBreakdown> = emptyList()
)
