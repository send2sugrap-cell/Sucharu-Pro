package com.sucharu.sucharupro.domain.model.inventory.stockout

/**
 * Immutable append-only audit record for stock out / issue operations (Module 07 Step 04).
 *
 * Audit events are write-once: they must never be edited or deleted after creation.
 */
data class InventoryStockOutActivityEvent(
    val eventId: String,
    val projectId: String,
    val stockOutId: String,
    val stockOutLineId: String? = null,
    val eventType: InventoryStockOutActivityType,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String,
    val metadata: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(stockOutId.isNotBlank()) { "Stock-out ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
