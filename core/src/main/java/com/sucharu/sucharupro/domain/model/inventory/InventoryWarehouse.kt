package com.sucharu.sucharupro.domain.model.inventory

/**
 * Primary physical warehouse master entity in Sucharu Pro (Module 07 Step 02).
 */
data class InventoryWarehouse(
    val id: String,
    val projectId: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val type: InventoryWarehouseType = InventoryWarehouseType.MAIN,
    val status: InventoryWarehouseStatus = InventoryWarehouseStatus.ACTIVE,
    val address: String? = null,
    val contactPerson: String? = null,
    val contactPhone: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String? = null
) {
    init {
        require(id.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(code.isNotBlank()) { "Warehouse code cannot be blank." }
        require(name.isNotBlank()) { "Warehouse name cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
        if (status == InventoryWarehouseStatus.ARCHIVED) {
            require(!archivedAt.isNullOrBlank()) { "archivedAt timestamp is required for ARCHIVED warehouses." }
        }
    }

    /**
     * Normalized uppercase warehouse code for project-scoped uniqueness checks.
     */
    val normalizedCode: String
        get() = code.trim().uppercase()

    val isTerminal: Boolean
        get() = status.isTerminal
}
