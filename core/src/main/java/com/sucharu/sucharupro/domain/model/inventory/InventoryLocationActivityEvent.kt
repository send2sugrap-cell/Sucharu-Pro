package com.sucharu.sucharupro.domain.model.inventory

/**
 * Audit event types for storage location management (Module 07 Step 02).
 */
enum class InventoryLocationActivityType(val defaultLabel: String) {
    LOCATION_CREATED("Location Created"),
    LOCATION_UPDATED("Location Updated"),
    LOCATION_ACTIVATED("Location Activated"),
    LOCATION_DEACTIVATED("Location Deactivated"),
    LOCATION_ARCHIVED("Location Archived"),
    LOCATION_PARENT_CHANGED("Location Parent Changed")
}

/**
 * Immutable audit log record for storage location operations (Module 07 Step 02).
 */
data class InventoryLocationActivityEvent(
    val eventId: String,
    val projectId: String,
    val warehouseId: String,
    val locationId: String,
    val eventType: InventoryLocationActivityType,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
