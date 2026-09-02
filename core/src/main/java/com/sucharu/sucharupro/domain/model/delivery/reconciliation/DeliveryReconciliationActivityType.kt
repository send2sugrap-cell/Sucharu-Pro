package com.sucharu.sucharupro.domain.model.delivery.reconciliation

enum class DeliveryReconciliationActivityType(val defaultLabel: String) {
    CREATED("Reconciliation Initialized"),
    CALCULATION_REFRESHED("Quantities Refreshed"),
    RECONCILIATION_STARTED("Reconciliation Started"),
    PARTIALLY_RECONCILED("Marked Partially Reconciled"),
    RECONCILED("Marked Reconciled"),
    DISCREPANCY_DETECTED("Discrepancy Detected"),
    DISPUTED("Marked Disputed"),
    RESOLUTION_STARTED("Discrepancy Resolution Started"),
    RESOLVED("Discrepancies Resolved"),
    CLOSED("Reconciliation Closed"),
    SETTLEMENT_UPDATED("Operational Settlement Updated")
}
