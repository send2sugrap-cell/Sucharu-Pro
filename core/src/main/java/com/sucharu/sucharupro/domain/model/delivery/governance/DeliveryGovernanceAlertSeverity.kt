package com.sucharu.sucharupro.domain.model.delivery.governance

/**
 * Severity level of a delivery governance exception.
 */
enum class DeliveryGovernanceAlertSeverity(val label: String) {
    INFO("Info"),
    WARNING("Warning"),
    CRITICAL("Critical")
}
