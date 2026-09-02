package com.sucharu.sucharupro.domain.model.inventory.receiving

/**
 * Immutable append-only audit record for stock receiving operations (Module 07 Step 03).
 *
 * Audit events are write-once: they must never be edited or deleted after creation.
 * Every important mutation in the receiving lifecycle produces one or more audit events,
 * each identifying the actor, timestamp, and context.
 */
data class InventoryReceivingActivityEvent(
    val eventId: String,
    val projectId: String,
    val receivingId: String,
    val receivingLineId: String? = null,
    val eventType: InventoryReceivingActivityType,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String,
    val metadata: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(receivingId.isNotBlank()) { "Receiving ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
