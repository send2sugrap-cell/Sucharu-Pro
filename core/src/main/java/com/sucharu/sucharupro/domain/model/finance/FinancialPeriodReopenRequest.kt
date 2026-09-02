package com.sucharu.sucharupro.domain.model.finance

/**
 * Status of a request to reopen a closed accounting period (Module 09 Step 08).
 */
enum class FinancialPeriodReopenStatus(val defaultLabel: String) {
    PENDING("Pending Admin Approval"),
    APPROVED("Approved & Reopened"),
    REJECTED("Rejected by Admin"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == APPROVED || this == REJECTED || this == CANCELLED
}

/**
 * Audit request entity for controlled reopening of a closed financial period (Module 09 Step 08).
 */
data class FinancialPeriodReopenRequest(
    val requestId: String,
    val requestNo: String,
    val projectId: String,
    val periodId: String,
    val requestedBy: String,
    val reason: String,
    val status: FinancialPeriodReopenStatus = FinancialPeriodReopenStatus.PENDING,
    val approvedBy: String? = null,
    val requestedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,
    val rejectedAt: Long? = null,
    val rejectionReason: String? = null
)
