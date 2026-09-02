package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Receivable Analytics & Customer Concentration (Module 09 Step 10).
 */
data class ReceivableAnalytics(
    val projectId: String,
    val totalReceivables: Money,
    val currentReceivables: Money,
    val overdueReceivables: Money,
    val overdue1To30: Money,
    val overdue31To60: Money,
    val overdue61To90: Money,
    val overdue90Plus: Money,
    val collectionRatePercent: Double? = null,
    val overduePercentage: Double? = null,
    val topCustomerExposures: List<CustomerReceivableExposure> = emptyList(),
    val riskLevel: FinancialRiskLevel = FinancialRiskLevel.LOW,
    val analyzedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}

data class CustomerReceivableExposure(
    val customerId: String,
    val customerName: String,
    val outstandingAmount: Money,
    val overdueAmount: Money,
    val percentageOfTotal: Double
)

enum class FinancialRiskLevel(val defaultLabel: String) {
    LOW("Low Risk"),
    MODERATE("Moderate Risk"),
    ELEVATED("Elevated Risk"),
    HIGH("High Risk"),
    CRITICAL("Critical Risk")
}
