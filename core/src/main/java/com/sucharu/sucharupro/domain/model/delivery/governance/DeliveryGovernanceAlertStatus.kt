package com.sucharu.sucharupro.domain.model.delivery.governance

/**
 * Controlled lifecycle status of a delivery governance alert.
 */
enum class DeliveryGovernanceAlertStatus(val label: String) {
    OPEN("Open"),
    ACKNOWLEDGED("Acknowledged"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed")
}
