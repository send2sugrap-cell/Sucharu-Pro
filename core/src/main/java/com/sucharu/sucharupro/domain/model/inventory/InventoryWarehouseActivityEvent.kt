package com.sucharu.sucharupro.domain.model.inventory

/**
 * Audit event types for warehouse management (Module 07 Step 02).
 */
enum class InventoryWarehouseActivityType(val defaultLabel: String) {
    WAREHOUSE_CREATED("Warehouse Created"),
    WAREHOUSE_UPDATED("Warehouse Updated"),
    WAREHOUSE_ACTIVATED("Warehouse Activated"),
    WAREHOUSE_DEACTIVATED("Warehouse Deactivated"),
    WAREHOUSE_ARCHIVED("Warehouse Archived")
}

/**
 * Immutable audit log record for warehouse operations (Module 07 Step 02).
 */
data class InventoryWarehouseActivityEvent(
    val eventId: String,
    val projectId: String,
    val warehouseId: String,
    val eventType: InventoryWarehouseActivityType,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
