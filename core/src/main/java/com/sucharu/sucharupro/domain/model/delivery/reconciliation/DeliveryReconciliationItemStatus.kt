package com.sucharu.sucharupro.domain.model.delivery.reconciliation

/**
 * Line-level status for Delivery Reconciliation Item (Module 08 Step 09).
 */
enum class DeliveryReconciliationItemStatus(val defaultLabel: String) {
    PENDING("Pending"),
    MATCHED("Matched"),
    DISCREPANCY("Discrepancy"),
    RECONCILED("Reconciled"),
    RESOLVED("Resolved"),
    CLOSED("Closed")
}
