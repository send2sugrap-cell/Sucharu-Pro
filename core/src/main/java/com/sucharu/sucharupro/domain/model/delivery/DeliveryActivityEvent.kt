package com.sucharu.sucharupro.domain.model.delivery

/**
 * Audit event for Delivery Order activities.
 *
 * @param activityId Unique identifier for the audit event.
 * @param projectId The project context.
 * @param deliveryOrderId Reference to the Delivery Order.
 * @param activityType The type of action performed.
 * @param performedBy ID of the user who performed the action.
 * @param performedAt Timestamp of the action.
 * @param details Optional free-form details about the event.
 * @param previousStatus Optional status label before the change.
 * @param newStatus Optional status label after the change.
 */
data class DeliveryActivityEvent(
    val activityId: String,
    val projectId: String,
    val deliveryOrderId: String,
    val activityType: DeliveryActivityType,
    val performedBy: String,
    val performedAt: Long,
    val details: String? = null,
    val previousStatus: String? = null,
    val newStatus: String? = null
) {
    init {
        require(activityId.isNotBlank()) { "Activity ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(performedBy.isNotBlank()) { "Performed By cannot be blank." }
        require(performedAt > 0) { "Performed At timestamp must be positive." }
    }
}
