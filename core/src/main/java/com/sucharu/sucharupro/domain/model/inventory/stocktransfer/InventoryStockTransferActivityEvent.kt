package com.sucharu.sucharupro.domain.model.inventory.stocktransfer

/**
 * Immutable append-only audit record for stock transfer operations (Module 07 Step 05).
 *
 * Audit events are write-once: they must never be edited or deleted after creation.
 */
data class InventoryStockTransferActivityEvent(
    val eventId: String,
    val projectId: String,
    val transferId: String,
    val transferLineId: String? = null,
    val eventType: InventoryStockTransferActivityType,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String,
    val metadata: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(transferId.isNotBlank()) { "Transfer ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
