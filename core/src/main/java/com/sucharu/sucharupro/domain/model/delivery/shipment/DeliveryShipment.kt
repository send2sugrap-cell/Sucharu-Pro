package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Aggregate root representing a physical Delivery Shipment (Module 08 Step 05).
 *
 * @param shipmentId Unique identifier for the shipment.
 * @param projectId Project boundary context.
 * @param shipmentNo Unique human-readable shipment number within the project scope.
 * @param deliveryOrderId Reference to parent Delivery Order.
 * @param deliveryChallanId Reference to parent Delivery Challan.
 * @param dispatchExecutionId Reference to parent Dispatch Execution.
 * @param verificationId Optional reference to Delivery Item Verification (Module 08 Step 04).
 * @param customerId Optional reference to customer recipient.
 * @param shipmentType Delivery / shipment method classification.
 * @param priority Fulfillment urgency.
 * @param carrierName Name of external carrier or internal delivery team.
 * @param carrierReference Carrier waybill / booking reference.
 * @param trackingNumber Tracking reference number.
 * @param originLocationId Dispatch source warehouse / location identifier.
 * @param destinationAddress Physical delivery destination address snapshot.
 * @param destinationContactName Recipient contact person snapshot.
 * @param destinationContactPhone Recipient contact phone snapshot.
 * @param destinationNotes Delivery instructions / gate directions snapshot.
 * @param estimatedDispatchAt Planned dispatch timestamp (epoch millis).
 * @param actualDispatchAt Actual dispatch timestamp (epoch millis).
 * @param estimatedDeliveryAt Expected delivery timestamp (epoch millis).
 * @param actualDeliveryAt Recorded delivery timestamp (epoch millis).
 * @param currentStatus Current tracking and operational lifecycle status.
 * @param notes Operational remarks.
 * @param createdBy User ID who created the shipment record.
 * @param createdAt Creation timestamp (epoch millis).
 * @param updatedBy User ID who last modified the shipment record.
 * @param updatedAt Modification timestamp (epoch millis).
 */
data class DeliveryShipment(
    val shipmentId: String,
    val projectId: String,
    val shipmentNo: String,
    val deliveryOrderId: String,
    val deliveryChallanId: String,
    val dispatchExecutionId: String,
    val verificationId: String? = null,
    val customerId: String? = null,
    val shipmentType: DeliveryShipmentType = DeliveryShipmentType.STANDARD,
    val priority: DeliveryShipmentPriority = DeliveryShipmentPriority.NORMAL,
    val carrierName: String? = null,
    val carrierReference: String? = null,
    val trackingNumber: String? = null,
    val originLocationId: String? = null,
    val destinationAddress: String? = null,
    val destinationContactName: String? = null,
    val destinationContactPhone: String? = null,
    val destinationNotes: String? = null,
    val estimatedDispatchAt: Long? = null,
    val actualDispatchAt: Long? = null,
    val estimatedDeliveryAt: Long? = null,
    val actualDeliveryAt: Long? = null,
    val currentStatus: DeliveryShipmentStatus = DeliveryShipmentStatus.DRAFT,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(shipmentId.isNotBlank()) { "Shipment ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(shipmentNo.isNotBlank()) { "Shipment Number cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(deliveryChallanId.isNotBlank()) { "Delivery Challan ID cannot be blank." }
        require(dispatchExecutionId.isNotBlank()) { "Dispatch Execution ID cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By user ID cannot be blank." }
        require(createdAt > 0) { "Created At timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated At timestamp cannot be earlier than Created At." }

        if (trackingNumber != null) {
            require(trackingNumber.isNotBlank()) { "Tracking Number cannot be blank if provided." }
        }
    }
}
