package com.sucharu.sucharupro.domain.model.delivery.governance

/**
 * Activity types for delivery governance audit events.
 */
enum class DeliveryGovernanceActivityType(val label: String) {
    ALERT_GENERATED("Alert Detected & Generated"),
    ALERT_ACKNOWLEDGED("Alert Acknowledged"),
    ALERT_RESOLVED("Alert Resolved"),
    ALERT_DISMISSED("Alert Dismissed")
}
