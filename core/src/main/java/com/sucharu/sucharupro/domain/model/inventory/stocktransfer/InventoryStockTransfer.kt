package com.sucharu.sucharupro.domain.model.inventory.stocktransfer

/**
 * Aggregate root entity for a stock transfer operation (Module 07 Step 05).
 *
 * Represents the top-level document capturing the movement of finished product
 * inventory between warehouses.
 *
 * Key constraints:
 * - A transfer is scoped to exactly one project.
 * - transferReference must be unique within a project.
 * - Terminal records (COMPLETED, CANCELLED) must not be mutated.
 */
data class InventoryStockTransfer(
    val transferId: String,
    val projectId: String,
    val transferReference: String,
    val fromWarehouseId: String,
    val toWarehouseId: String,
    val transferDate: String,
    val status: InventoryStockTransferStatus = InventoryStockTransferStatus.DRAFT,
    val expectedTotalQuantity: Int = 0,
    val transferredTotalQuantity: Int = 0,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val approvedAt: String? = null,
    val approvedBy: String? = null,
    val completedAt: String? = null,
    val completedBy: String? = null,
    val cancelledAt: String? = null,
    val cancelledBy: String? = null
) {
    init {
        require(transferId.isNotBlank()) { "Transfer ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(transferReference.isNotBlank()) { "Transfer reference cannot be blank." }
        require(fromWarehouseId.isNotBlank()) { "From Warehouse ID cannot be blank." }
        require(toWarehouseId.isNotBlank()) { "To Warehouse ID cannot be blank." }
        require(fromWarehouseId != toWarehouseId) { "Source and destination warehouses must be different." }
        require(transferDate.isNotBlank()) { "Transfer date cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
        require(expectedTotalQuantity >= 0) { "expectedTotalQuantity cannot be negative." }
        require(transferredTotalQuantity >= 0) { "transferredTotalQuantity cannot be negative." }

        if (status == InventoryStockTransferStatus.APPROVED) {
            require(!approvedAt.isNullOrBlank()) { "approvedAt timestamp is required for APPROVED transfers." }
        }
        if (status == InventoryStockTransferStatus.COMPLETED) {
            require(!completedAt.isNullOrBlank()) { "completedAt timestamp is required for COMPLETED transfers." }
        }
        if (status == InventoryStockTransferStatus.CANCELLED) {
            require(!cancelledAt.isNullOrBlank()) { "cancelledAt timestamp is required for CANCELLED transfers." }
        }
    }

    /**
     * Normalized reference for project-scoped uniqueness checking.
     */
    val normalizedReference: String
        get() = transferReference.trim().uppercase()

    /**
     * True if this transfer is in a terminal state and must not be mutated.
     */
    val isTerminal: Boolean
        get() = status.isTerminal
}
