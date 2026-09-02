package com.sucharu.sucharupro.domain.model.inventory.analytics

/**
 * Immutable audit record for inventory analytics and governance events (Module 07 Step 10).
 */
data class InventoryAnalyticsActivityEvent(
    val eventId: String,
    val projectId: String,
    val targetId: String? = null,
    val eventType: InventoryAnalyticsActivityType,
    val actorId: String,
    val actorName: String? = null,
    val description: String,
    val timestamp: String,
    val metadata: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
