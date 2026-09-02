package com.sucharu.sucharupro.domain.model.inventory.adjustment

/**
 * Aggregate root entity for a stock adjustment operation (Module 07 Step 06).
 *
 * Represents the top-level document capturing corrections to product inventory
 * levels within a warehouse (e.g., physical counts, damages).
 *
 * Key constraints:
 * - An adjustment is scoped to exactly one project and one warehouse.
 * - adjustmentReference must be unique within a project.
 * - Terminal records (COMPLETED, CANCELLED) must not be mutated.
 */
data class InventoryStockAdjustment(
    val adjustmentId: String,
    val projectId: String,
    val adjustmentReference: String,
    val warehouseId: String,
    val adjustmentDate: String,
    val status: InventoryStockAdjustmentStatus = InventoryStockAdjustmentStatus.DRAFT,
    val totalItemsAdjusted: Int = 0,
    val totalQuantityChange: Int = 0,
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
        require(adjustmentId.isNotBlank()) { "Adjustment ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(adjustmentReference.isNotBlank()) { "Adjustment reference cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(adjustmentDate.isNotBlank()) { "Adjustment date cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
        require(totalItemsAdjusted >= 0) { "totalItemsAdjusted cannot be negative." }

        if (status == InventoryStockAdjustmentStatus.APPROVED) {
            require(!approvedAt.isNullOrBlank()) { "approvedAt timestamp is required for APPROVED adjustments." }
        }
        if (status == InventoryStockAdjustmentStatus.COMPLETED) {
            require(!completedAt.isNullOrBlank()) { "completedAt timestamp is required for COMPLETED adjustments." }
        }
        if (status == InventoryStockAdjustmentStatus.CANCELLED) {
            require(!cancelledAt.isNullOrBlank()) { "cancelledAt timestamp is required for CANCELLED adjustments." }
        }
    }

    /**
     * Normalized reference for project-scoped uniqueness checking.
     */
    val normalizedReference: String
        get() = adjustmentReference.trim().uppercase()

    /**
     * True if this adjustment is in a terminal state and must not be mutated.
     */
    val isTerminal: Boolean
        get() = status.isTerminal
}
