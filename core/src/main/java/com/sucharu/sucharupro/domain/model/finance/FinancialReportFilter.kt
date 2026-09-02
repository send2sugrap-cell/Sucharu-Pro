package com.sucharu.sucharupro.domain.model.finance

/**
 * Strongly typed, project-scoped report filter (Module 09 Step 09).
 *
 * [projectId] is ALWAYS required. No query may bypass project isolation.
 * All other fields are optional and additive.
 *
 * Never set projectId to blank or "*" — the repository layer rejects such attempts.
 */
data class FinancialReportFilter(
    val projectId: String,
    val reportPeriod: FinancialReportPeriod = FinancialReportPeriod.CurrentMonth,
    /** Explicit timestamp range override, derived from [reportPeriod] by the calculator. */
    val resolvedStartDate: Long? = null,
    val resolvedEndDate: Long? = null,
    val accountingPeriodId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val expenseCategoryId: String? = null,
    val transactionType: FinancialTransactionType? = null,
    val transactionStatus: FinancialTransactionStatus? = null,
    val page: Int = 0,
    val pageSize: Int = 50
) {
    init {
        require(projectId.isNotBlank()) { "Report filter projectId cannot be blank." }
        require(page >= 0) { "Page index cannot be negative." }
        require(pageSize in 1..500) { "Page size must be between 1 and 500." }
        if (resolvedStartDate != null && resolvedEndDate != null) {
            require(resolvedEndDate >= resolvedStartDate) { "Resolved end date cannot precede start date." }
        }
    }
}
