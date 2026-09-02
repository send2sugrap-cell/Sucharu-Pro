package com.sucharu.sucharupro.domain.model.inventory.ledger

/**
 * Audit record for ledger-related activities (Module 07 Step 09).
 */
data class InventoryLedgerActivityEvent(
    val eventId: String,
    val projectId: String,
    val activityType: InventoryLedgerActivityType,
    val performedBy: String,
    val occurredAt: String,
    val metadata: Map<String, String> = emptyMap(),
    val description: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(performedBy.isNotBlank()) { "Performed by actor cannot be blank" }
        require(occurredAt.isNotBlank()) { "occurredAt timestamp cannot be blank" }
    }
}
