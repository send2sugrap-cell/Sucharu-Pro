package com.sucharu.sucharupro.domain.model.inventory.receiving

/**
 * Aggregate root entity for a stock receiving operation (Module 07 Step 03).
 *
 * Represents the top-level document capturing the receipt of finished product
 * inventory into a specific warehouse. Each receiving operation consists of
 * one or more [InventoryReceivingLine] items.
 *
 * Key constraints:
 * - A receiving record is scoped to exactly one project and one warehouse.
 * - The receivingReference must be unique within a project.
 * - Terminal records (COMPLETED, CANCELLED, REJECTED) must not be mutated.
 */
data class InventoryReceiving(
    val receivingId: String,
    val projectId: String,
    val receivingReference: String,
    val warehouseId: String,
    val receivingDate: String,
    val status: InventoryReceivingStatus = InventoryReceivingStatus.DRAFT,
    val sourceReference: String? = null,
    val sourceType: String? = null,
    val expectedTotalQuantity: Int = 0,
    val acceptedTotalQuantity: Int = 0,
    val rejectedTotalQuantity: Int = 0,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String? = null,
    val completedBy: String? = null,
    val cancelledAt: String? = null,
    val cancelledBy: String? = null
) {
    init {
        require(receivingId.isNotBlank()) { "Receiving ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(receivingReference.isNotBlank()) { "Receiving reference cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(receivingDate.isNotBlank()) { "Receiving date cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
        require(expectedTotalQuantity >= 0) { "expectedTotalQuantity cannot be negative." }
        require(acceptedTotalQuantity >= 0) { "acceptedTotalQuantity cannot be negative." }
        require(rejectedTotalQuantity >= 0) { "rejectedTotalQuantity cannot be negative." }
        if (status == InventoryReceivingStatus.COMPLETED) {
            require(!completedAt.isNullOrBlank()) { "completedAt timestamp is required for COMPLETED receivings." }
        }
        if (status == InventoryReceivingStatus.CANCELLED) {
            require(!cancelledAt.isNullOrBlank()) { "cancelledAt timestamp is required for CANCELLED receivings." }
        }
    }

    /**
     * Normalized reference for project-scoped uniqueness checking.
     */
    val normalizedReference: String
        get() = receivingReference.trim().uppercase()

    /**
     * True if this receiving is in a terminal state and must not be mutated.
     */
    val isTerminal: Boolean
        get() = status.isTerminal
}
