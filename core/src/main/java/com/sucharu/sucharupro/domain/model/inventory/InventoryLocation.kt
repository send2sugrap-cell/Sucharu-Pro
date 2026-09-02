package com.sucharu.sucharupro.domain.model.inventory

/**
 * Physical storage location entity within a warehouse (Module 07 Step 02).
 */
data class InventoryLocation(
    val id: String,
    val projectId: String,
    val warehouseId: String,
    val parentLocationId: String? = null,
    val code: String,
    val name: String,
    val description: String? = null,
    val type: InventoryLocationType = InventoryLocationType.BIN,
    val status: InventoryLocationStatus = InventoryLocationStatus.ACTIVE,
    val capacity: Double? = null,
    val capacityUnit: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String? = null
) {
    init {
        require(id.isNotBlank()) { "Location ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(code.isNotBlank()) { "Location code cannot be blank." }
        require(name.isNotBlank()) { "Location name cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
        if (capacity != null) {
            require(capacity >= 0.0) { "Capacity cannot be negative (was $capacity)." }
        }
        if (status == InventoryLocationStatus.ARCHIVED) {
            require(!archivedAt.isNullOrBlank()) { "archivedAt timestamp is required for ARCHIVED locations." }
        }
    }

    /**
     * Normalized uppercase location code for warehouse-scoped uniqueness checks.
     */
    val normalizedCode: String
        get() = code.trim().uppercase()

    val isTerminal: Boolean
        get() = status.isTerminal
}
