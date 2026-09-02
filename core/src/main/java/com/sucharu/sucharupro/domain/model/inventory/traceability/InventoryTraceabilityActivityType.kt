package com.sucharu.sucharupro.domain.model.inventory.traceability

/**
 * Enumeration of activity types for batch and lot traceability (Module 07 Step 07).
 */
enum class InventoryTraceabilityActivityType(val defaultLabel: String) {
    REGISTERED("Registered"),
    STATUS_CHANGED("Status Changed"),
    HOLD_PLACED("Hold Placed"),
    HOLD_RELEASED("Hold Released"),
    CLOSED("Closed")
}
