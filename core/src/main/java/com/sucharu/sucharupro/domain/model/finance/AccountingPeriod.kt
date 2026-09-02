package com.sucharu.sucharupro.domain.model.finance

/**
 * Accounting Period entity managing fiscal and reporting cycles with period-lock enforcement (Module 09 Step 08).
 */
data class AccountingPeriod(
    val periodId: String,
    val periodNo: String,
    val projectId: String,
    val periodName: String,
    val startDate: Long,
    val endDate: Long,
    val status: AccountingPeriodStatus = AccountingPeriodStatus.OPEN,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val closingInitiatedBy: String? = null,
    val closingInitiatedAt: Long? = null,
    val closedBy: String? = null,
    val closedAt: Long? = null,
    val reopenedBy: String? = null,
    val reopenedAt: Long? = null,
    val reopenReason: String? = null,
    val closingReference: String? = null,
    val version: Int = 1
)
