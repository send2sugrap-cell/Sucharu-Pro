package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Reverse logistics shipment model tracking the physical transit of returned items (Module 08 Step 07).
 */
data class DeliveryReturnShipment(
    val reverseShipmentId: String,
    val returnId: String,
    val projectId: String,
    val carrierName: String,
    val trackingNumber: String? = null,
    val pickupAddress: String? = null,
    val destinationAddress: String? = null,
    val scheduledPickupAt: Long? = null,
    val pickedUpAt: Long? = null,
    val receivedAt: Long? = null,
    val status: DeliveryReturnShipmentStatus = DeliveryReturnShipmentStatus.DRAFT,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(reverseShipmentId.isNotBlank()) { "Reverse Shipment ID cannot be blank." }
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(carrierName.isNotBlank()) { "Carrier Name cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation." }
    }
}
