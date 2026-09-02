package com.sucharu.sucharupro.domain.model.inventory.traceability

/**
 * Lifecycle states for batches and lots in inventory traceability (Module 07 Step 07).
 */
enum class InventoryTraceabilityStatus(
    val defaultLabel: String,
    val isTerminal: Boolean = false
) {
    ACTIVE("Active", isTerminal = false),
    HOLD("On Hold", isTerminal = false),
    EXHAUSTED("Exhausted", isTerminal = true),
    CLOSED("Closed", isTerminal = true),
    CANCELLED("Cancelled", isTerminal = true)
}
