package com.sucharu.sucharupro.domain.model.inventory.reorder

/**
 * Enumeration of audit event types for reorder and stock level operations (Module 07 Step 08).
 */
enum class InventoryReorderActivityType(val defaultLabel: String) {
    POLICY_CREATED("Policy Created"),
    POLICY_UPDATED("Policy Updated"),
    ALERT_CREATED("Alert Created"),
    ALERT_ACKNOWLEDGED("Alert Acknowledged"),
    ALERT_RESOLVED("Alert Resolved"),
    ALERT_DISMISSED("Alert Dismissed")
}
