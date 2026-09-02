package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Aggregated summary of vendor payable obligations and overdue liabilities (Module 09 Step 04).
 */
data class VendorPayableSummary(
    val projectId: String,
    val vendorId: String? = null,
    val totalPayablesCount: Int = 0,
    val openPayablesCount: Int = 0,
    val overduePayablesCount: Int = 0,
    val totalOriginalAmount: Money = Money.ZERO,
    val totalSettledAmount: Money = Money.ZERO,
    val totalOutstandingPayable: Money = Money.ZERO,
    val totalOverdueAmount: Money = Money.ZERO,
    val agingCurrent: Money = Money.ZERO,
    val aging1To30Days: Money = Money.ZERO,
    val aging31To60Days: Money = Money.ZERO,
    val aging61To90Days: Money = Money.ZERO,
    val agingOver90Days: Money = Money.ZERO
)
