package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Supplier Payment & Settlement Analytics (Module 09 Step 10).
 */
data class SupplierPaymentAnalytics(
    val projectId: String,
    val totalPayableCreated: Money,
    val totalPayableSettled: Money,
    val totalOutstandingPayable: Money,
    val settlementRatePercent: Double? = null,
    val vendorPaymentSummaries: List<VendorPaymentSummary> = emptyList(),
    val trend: FinancialKpiTrend = FinancialKpiTrend.STABLE,
    val analyzedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
