package com.sucharu.sucharupro.domain.model.finance

/**
 * Activity event types for financial reconciliation, period closing, and control auditing (Module 09 Step 08).
 */
enum class FinancialReconciliationActivityType(val defaultLabel: String) {
    PERIOD_CREATED("Accounting Period Created"),
    PERIOD_UPDATED("Accounting Period Updated"),
    PERIOD_CLOSING_STARTED("Period Closing Process Started"),
    PERIOD_CLOSING_VALIDATED("Period Closing Checklist Validated"),
    PERIOD_CLOSED("Accounting Period Closed & Locked"),
    PERIOD_REOPEN_REQUESTED("Period Reopen Requested"),
    PERIOD_REOPEN_APPROVED("Period Reopen Approved by Admin"),
    PERIOD_REOPENED("Accounting Period Reopened for Audit"),
    RECONCILIATION_CREATED("Reconciliation Created"),
    RECONCILIATION_STARTED("Reconciliation Calculation Started"),
    RECONCILIATION_MATCHED("Reconciliation Fully Balanced"),
    RECONCILIATION_MISMATCHED("Reconciliation Discrepancy Found"),
    RECONCILIATION_APPROVED("Reconciliation Approved"),
    RECONCILIATION_CLOSED("Reconciliation Closed & Locked"),
    DISCREPANCY_CREATED("Financial Discrepancy Detected"),
    DISCREPANCY_RESOLVED("Financial Discrepancy Resolved"),
    DISCREPANCY_WAIVED("Financial Discrepancy Waived by Admin"),
    CLOSING_SNAPSHOT_CREATED("Immutable Closing Snapshot Generated")
}

/**
 * Immutable audit trail event model for financial control and reconciliation activities (Module 09 Step 08).
 */
data class FinancialReconciliationActivityEvent(
    val eventId: String,
    val entityId: String,
    val projectId: String,
    val activityType: FinancialReconciliationActivityType,
    val actorId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
