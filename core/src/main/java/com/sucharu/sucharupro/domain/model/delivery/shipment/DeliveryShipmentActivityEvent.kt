package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Immutable audit log record for Delivery Shipment lifecycle operations (Module 08 Step 05).
 * Append-only.
 *
 * @param activityId Unique identifier for this audit event.
 * @param projectId Project boundary context.
 * @param shipmentId Associated shipment identifier.
 * @param activityType Type classification of the activity performed.
 * @param performedBy User ID who performed the operation.
 * @param performedAt Timestamp of execution (epoch millis).
 * @param previousStatus State before the operation if applicable.
 * @param newStatus State after the operation if applicable.
 * @param details Human-readable operation details.
 */
data class DeliveryShipmentActivityEvent(
    val activityId: String,
    val projectId: String,
    val shipmentId: String,
    val activityType: DeliveryShipmentActivityType,
    val performedBy: String,
    val performedAt: Long = System.currentTimeMillis(),
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val details: String? = null
) {
    init {
        require(activityId.isNotBlank()) { "Activity ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(shipmentId.isNotBlank()) { "Shipment ID cannot be blank." }
        require(performedBy.isNotBlank()) { "Performed By user ID cannot be blank." }
        require(performedAt > 0) { "Performed At timestamp must be positive." }
    }
}
