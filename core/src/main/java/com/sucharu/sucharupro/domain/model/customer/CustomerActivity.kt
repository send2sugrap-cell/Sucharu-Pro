package com.sucharu.sucharupro.domain.model.customer

/**
 * Lightweight domain model representing a customer-scoped management activity event.
 *
 * @param id Unique activity record identifier.
 * @param customerId Reference to the customer entity.
 * @param type Activity category.
 * @param description Human-readable description of the activity.
 * @param timestamp ISO 8601 event timestamp.
 * @param actorName Optional staff / user name who performed the action.
 */
data class CustomerActivity(
    val id: String,
    val customerId: String,
    val type: CustomerActivityType,
    val description: String,
    val timestamp: String,
    val actorName: String? = null
) {
    init {
        require(id.isNotBlank()) { "Activity ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
    }
}
