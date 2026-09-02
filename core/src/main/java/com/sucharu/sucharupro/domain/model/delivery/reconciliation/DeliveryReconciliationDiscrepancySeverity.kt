package com.sucharu.sucharupro.domain.model.delivery.reconciliation

enum class DeliveryReconciliationDiscrepancySeverity(val defaultLabel: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}
