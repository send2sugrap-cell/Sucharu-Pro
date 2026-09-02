package com.sucharu.sucharupro.domain.model.delivery.reconciliation

/**
 * Lifecycle status for Delivery Reconciliation (Module 08 Step 09).
 */
enum class DeliveryReconciliationStatus(val defaultLabel: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    PARTIALLY_RECONCILED("Partially Reconciled"),
    REQUIRES_REVIEW("Requires Review"),
    RECONCILED("Reconciled"),
    DISPUTED("Disputed"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    val isTerminal: Boolean
        get() = this == CLOSED

    val canEdit: Boolean
        get() = this != CLOSED

    val allowsResolution: Boolean
        get() = this == DISPUTED || this == REQUIRES_REVIEW || this == PARTIALLY_RECONCILED
}
