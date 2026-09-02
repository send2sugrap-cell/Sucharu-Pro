package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Immutable tracking event in the chronological history of a Delivery Shipment (Module 08 Step 05).
 * Append-only.
 *
 * @param eventId Unique identifier for this tracking event.
 * @param projectId Project boundary context.
 * @param shipmentId Associated shipment identifier.
 * @param eventType Classification of tracking event.
 * @param status Snapshot of shipment status at the time of event.
 * @param eventTime Timestamp when event occurred (epoch millis).
 * @param locationText Optional textual location description (e.g., "Dhaka Central Hub", "Mirpur Warehouse").
 * @param description Optional event description or remark.
 * @param actorId User or carrier agent ID recording the event.
 * @param createdAt Creation timestamp (epoch millis).
 */
data class DeliveryShipmentEvent(
    val eventId: String,
    val projectId: String,
    val shipmentId: String,
    val eventType: DeliveryShipmentEventType,
    val status: DeliveryShipmentStatus,
    val eventTime: Long = System.currentTimeMillis(),
    val locationText: String? = null,
    val description: String? = null,
    val actorId: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(shipmentId.isNotBlank()) { "Shipment ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(eventTime > 0) { "Event Time must be positive." }
        require(createdAt > 0) { "Created At must be positive." }
    }
}
