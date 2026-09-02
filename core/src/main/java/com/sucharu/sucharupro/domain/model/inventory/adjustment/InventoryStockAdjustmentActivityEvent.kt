package com.sucharu.sucharupro.domain.model.inventory.adjustment

/**
 * Immutable append-only audit record for stock adjustment operations (Module 07 Step 06).
 *
 * Audit events are write-once: they must never be edited or deleted after creation.
 */
data class InventoryStockAdjustmentActivityEvent(
    val eventId: String,
    val projectId: String,
    val adjustmentId: String,
    val adjustmentLineId: String? = null,
    val eventType: InventoryStockAdjustmentActivityType,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String,
    val metadata: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(adjustmentId.isNotBlank()) { "Adjustment ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
