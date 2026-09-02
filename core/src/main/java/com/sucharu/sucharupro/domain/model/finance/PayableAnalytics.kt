package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Payable Analytics & Supplier Concentration (Module 09 Step 10).
 */
data class PayableAnalytics(
    val projectId: String,
    val totalPayables: Money,
    val currentPayables: Money,
    val overduePayables: Money,
    val overdue1To30: Money,
    val overdue31To60: Money,
    val overdue61To90: Money,
    val overdue90Plus: Money,
    val settlementRatePercent: Double? = null,
    val overduePercentage: Double? = null,
    val topSupplierExposures: List<SupplierPayableExposure> = emptyList(),
    val riskLevel: FinancialRiskLevel = FinancialRiskLevel.LOW,
    val analyzedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}

data class SupplierPayableExposure(
    val vendorId: String,
    val vendorName: String,
    val outstandingAmount: Money,
    val overdueAmount: Money,
    val percentageOfTotal: Double
)
