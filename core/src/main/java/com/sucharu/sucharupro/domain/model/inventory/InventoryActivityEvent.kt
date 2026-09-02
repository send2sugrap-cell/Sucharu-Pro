package com.sucharu.sucharupro.domain.model.inventory

/**
 * Enumeration of audit event types for inventory domain operations (Module 07 Step 01).
 */
enum class InventoryActivityType(val defaultLabel: String) {
    PRODUCT_CREATED("Product Created"),
    PRODUCT_UPDATED("Product Updated"),
    PRODUCT_ACTIVATED("Product Activated"),
    PRODUCT_DEACTIVATED("Product Deactivated"),
    CATEGORY_CREATED("Category Created"),
    CATEGORY_UPDATED("Category Updated"),
    CATEGORY_ACTIVATED("Category Activated"),
    CATEGORY_DEACTIVATED("Category Deactivated")
}

/**
 * Immutable audit event capturing an administrative or operational change in the inventory domain.
 */
data class InventoryActivityEvent(
    val eventId: String,
    val eventType: InventoryActivityType,
    val targetId: String,
    val targetType: String,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(targetId.isNotBlank()) { "Target ID cannot be blank." }
        require(targetType.isNotBlank()) { "Target type cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
