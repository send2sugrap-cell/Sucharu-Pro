package com.sucharu.sucharupro.domain.model.dashboard

/**
 * Operational alert counts for the Sucharu Pro Dashboard.
 *
 * Aggregates real-time operational counts across all active jobs in the shop.
 * Each field represents the number of jobs/items currently requiring attention
 * in that specific operational category.
 *
 * Design principles:
 *  - All fields are counts (Int) — no business logic or computation here.
 *  - No UI types, no Compose dependencies.
 *  - Does NOT contain the actual list of jobs — use [DashboardJobSummary] lists for that.
 *  - Backed by [ProductionStageType] semantics (QC stages, production stages, etc.)
 *    but does not directly reference enum values to keep the model decoupled.
 *
 * Dashboard UI uses these counts for:
 *  - Badge/counter display on alert cards
 *  - Priority color indicators
 *  - Notification dot on bottom nav items
 *
 * Future: each count will correspond to a filterable job list query in the
 * respective feature module (Order, Production, QC, Inventory, Finance).
 */
data class DashboardOperationalAlerts(

    /**
     * Jobs currently waiting for customer design approval.
     * Corresponds to [ProductionStageType.APPROVAL] stage with [ProductionStageStatus.PENDING].
     */
    val pendingApprovalCount: Int,

    /**
     * Jobs at a QC checkpoint ([ProductionStageType.QC] or [ProductionStageType.FINAL_QC])
     * that have not yet been inspected.
     */
    val qcPendingCount: Int,

    /**
     * Jobs with [OrderStatusType.READY] status that have a confirmed delivery due today
     * but have not yet been dispatched.
     */
    val deliveryPendingCount: Int,

    /**
     * Count of finished product SKUs currently at [StockStatusType.LOW_STOCK]
     * or [StockStatusType.OUT_OF_STOCK].
     * Corresponds to the count of [DashboardInventoryAlert] entries.
     */
    val lowStockAlertCount: Int,

    /**
     * Count of customer orders with overdue or unpaid invoices
     * (i.e. [PaymentStatusType.OVERDUE] or [PaymentStatusType.UNPAID] past delivery date).
     */
    val outstandingPaymentCount: Int,

    /**
     * Count of active vendor payables/bills that are due or overdue.
     * Used for Vendor Due alert on dashboard. Full vendor module is a later module.
     */
    val vendorDueCount: Int,

    /**
     * Count of jobs currently in the replacement/rework cycle.
     * A job enters replacement when a delivered product is returned due to defect.
     * Full replacement workflow is implemented in a future module.
     */
    val replacementPendingCount: Int,

    /**
     * Count of production jobs that have missed their scheduled delivery date
     * and are still in an active [ProductionStageType] stage.
     */
    val delayedJobsCount: Int
) {
    /**
     * Total number of active alerts requiring immediate attention.
     * Excludes vendor due (lower priority than production/delivery alerts).
     */
    val totalCriticalAlerts: Int
        get() = pendingApprovalCount +
                qcPendingCount +
                deliveryPendingCount +
                lowStockAlertCount +
                outstandingPaymentCount +
                replacementPendingCount +
                delayedJobsCount

    /**
     * Whether any alert requires immediate attention.
     */
    val hasAnyAlert: Boolean
        get() = totalCriticalAlerts > 0 || vendorDueCount > 0

    companion object {
        /** A zero-state alerts object — used as initial/empty state before data loads. */
        val EMPTY = DashboardOperationalAlerts(
            pendingApprovalCount = 0,
            qcPendingCount = 0,
            deliveryPendingCount = 0,
            lowStockAlertCount = 0,
            outstandingPaymentCount = 0,
            vendorDueCount = 0,
            replacementPendingCount = 0,
            delayedJobsCount = 0
        )
    }
}
