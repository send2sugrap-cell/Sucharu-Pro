package com.sucharu.sucharupro.domain.model.inventory.stockout

/**
 * Aggregate root entity for a stock out / issue operation (Module 07 Step 04).
 *
 * Represents the top-level document capturing the issuance of finished product
 * inventory from a specific warehouse. Each stock-out operation consists of
 * one or more [InventoryStockOutLine] items.
 *
 * Key constraints:
 * - A stock-out record is scoped to exactly one project and one warehouse.
 * - The stockOutReference must be unique within a project.
 * - Terminal records (COMPLETED, CANCELLED) must not be mutated.
 */
data class InventoryStockOut(
    val stockOutId: String,
    val projectId: String,
    val stockOutReference: String,
    val warehouseId: String,
    val stockOutDate: String,
    val status: InventoryStockOutStatus = InventoryStockOutStatus.DRAFT,
    val issueType: InventoryIssueType = InventoryIssueType.PRODUCTION,
    val sourceReference: String? = null,
    val expectedTotalQuantity: Int = 0,
    val issuedTotalQuantity: Int = 0,
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
        require(stockOutId.isNotBlank()) { "Stock-out ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(stockOutReference.isNotBlank()) { "Stock-out reference cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(stockOutDate.isNotBlank()) { "Stock-out date cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
        require(expectedTotalQuantity >= 0) { "expectedTotalQuantity cannot be negative." }
        require(issuedTotalQuantity >= 0) { "issuedTotalQuantity cannot be negative." }
        if (status == InventoryStockOutStatus.COMPLETED) {
            require(!completedAt.isNullOrBlank()) { "completedAt timestamp is required for COMPLETED stock-outs." }
        }
        if (status == InventoryStockOutStatus.CANCELLED) {
            require(!cancelledAt.isNullOrBlank()) { "cancelledAt timestamp is required for CANCELLED stock-outs." }
        }
    }

    /**
     * Normalized reference for project-scoped uniqueness checking.
     */
    val normalizedReference: String
        get() = stockOutReference.trim().uppercase()

    /**
     * True if this stock-out is in a terminal state and must not be mutated.
     */
    val isTerminal: Boolean
        get() = status.isTerminal
}
