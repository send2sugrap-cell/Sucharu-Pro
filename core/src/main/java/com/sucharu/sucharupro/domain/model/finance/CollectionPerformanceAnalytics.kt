package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Collection Performance Analytics (Module 09 Step 10).
 */
data class CollectionPerformanceAnalytics(
    val projectId: String,
    val totalInvoicedAmount: Money,
    val totalCollectedAmount: Money,
    val totalOutstandingAmount: Money,
    val collectionRatePercent: Double? = null,
    val overdueCollectionRatePercent: Double? = null,
    val customerCollectionRankings: List<CustomerCollectionRanking> = emptyList(),
    val trend: FinancialKpiTrend = FinancialKpiTrend.STABLE,
    val analyzedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}

data class CustomerCollectionRanking(
    val customerId: String,
    val customerName: String,
    val totalInvoiced: Money,
    val totalCollected: Money,
    val outstandingAmount: Money,
    val collectionRatePercent: Double
)
