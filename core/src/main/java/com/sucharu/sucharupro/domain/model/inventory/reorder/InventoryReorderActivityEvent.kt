package com.sucharu.sucharupro.domain.model.inventory.reorder

/**
 * Immutable audit record for reorder alert and stock level policy operations (Module 07 Step 08).
 */
data class InventoryReorderActivityEvent(
    val eventId: String,
    val projectId: String,
    val alertId: String? = null,
    val policyId: String? = null,
    val eventType: InventoryReorderActivityType,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String,
    val metadata: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
        
        // Ensure either alertId or policyId is present if relevant to the event type
        // This is a soft check as some events might be general, but usually they target one.
    }
}
