package com.sucharu.sucharupro.domain.model.finance

/**
 * Filter for financial analytics and governance queries (Module 09 Step 10).
 *
 * Project isolation is strictly mandatory: [projectId] cannot be blank.
 */
data class AnalyticsFilter(
    val projectId: String,
    val reportPeriod: FinancialReportPeriod = FinancialReportPeriod.CurrentMonth,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val expenseCategoryId: String? = null
) {
    init {
        require(projectId.isNotBlank()) { "Analytics filter projectId cannot be blank." }
        if (customStartDate != null && customEndDate != null) {
            require(customEndDate >= customStartDate) { "Custom end date cannot precede start date." }
        }
    }
}
