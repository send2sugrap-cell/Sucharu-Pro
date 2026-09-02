package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Aggregated summary of customer receivable obligations and overdue exposures (Module 09 Step 02).
 */
data class CustomerDueSummary(
    val projectId: String,
    val customerId: String? = null,
    val totalReceivablesCount: Int = 0,
    val openReceivablesCount: Int = 0,
    val overdueReceivablesCount: Int = 0,
    val totalOriginalAmount: Money = Money.ZERO,
    val totalSettledAmount: Money = Money.ZERO,
    val totalOutstandingDue: Money = Money.ZERO,
    val totalOverdueAmount: Money = Money.ZERO,
    val agingCurrent: Money = Money.ZERO,
    val aging1To30Days: Money = Money.ZERO,
    val aging31To60Days: Money = Money.ZERO,
    val aging61To90Days: Money = Money.ZERO,
    val agingOver90Days: Money = Money.ZERO
)
