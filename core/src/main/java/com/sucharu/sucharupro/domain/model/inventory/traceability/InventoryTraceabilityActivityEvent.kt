package com.sucharu.sucharupro.domain.model.inventory.traceability

/**
 * Immutable audit event capturing changes in batch or lot traceability (Module 07 Step 07).
 */
data class InventoryTraceabilityActivityEvent(
    val eventId: String,
    val projectId: String,
    val eventType: InventoryTraceabilityActivityType,
    val targetId: String,
    val targetType: String, // e.g., "BATCH", "LOT"
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(targetId.isNotBlank()) { "Target ID cannot be blank." }
        require(targetType.isNotBlank()) { "Target type cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
