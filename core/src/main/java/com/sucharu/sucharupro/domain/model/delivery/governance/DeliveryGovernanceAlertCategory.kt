package com.sucharu.sucharupro.domain.model.delivery.governance

/**
 * Categories of operational exceptions and risks monitored by Delivery Governance.
 */
enum class DeliveryGovernanceAlertCategory(val label: String) {
    OVERDUE_DELIVERY("Overdue Delivery"),
    MISSING_POD("Missing Proof of Delivery"),
    POD_REJECTED("Rejected Proof of Delivery"),
    RECONCILIATION_DISCREPANCY("Reconciliation Discrepancy"),
    EXCESSIVE_RETURN("Excessive Return / Rejection"),
    UNRESOLVED_DELIVERY("Unresolved Delivery"),
    SLA_BREACH("Delivery SLA Breach"),
    SUSPICIOUS_STATUS_SEQUENCE("Suspicious Status Sequence")
}
