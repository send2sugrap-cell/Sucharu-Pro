package com.sucharu.sucharupro.domain.model.delivery

/**
 * Aggregate root representing a Delivery Order in Sucharu Pro.
 *
 * @param deliveryOrderId Unique identifier for the delivery order.
 * @param projectId The project this delivery order belongs to.
 * @param deliveryOrderNo Human-readable order number.
 * @param customerId Optional ID of the customer receiving the delivery.
 * @param sourceReferenceId Optional ID of the source document (e.g., Sales Order ID).
 * @param sourceReferenceType Optional type of the source document.
 * @param deliveryType The type of delivery (e.g., Customer, Internal).
 * @param priority Fulfillment urgency.
 * @param status Current lifecycle status.
 * @param requestedDeliveryDate Targeted date for delivery (epoch millis).
 * @param notes Optional operational remarks.
 * @param createdBy ID of the user who created the order.
 * @param createdAt Timestamp when created (epoch millis).
 * @param updatedAt Timestamp when last modified (epoch millis).
 */
data class DeliveryOrder(
    val deliveryOrderId: String,
    val projectId: String,
    val deliveryOrderNo: String,
    val customerId: String?,
    val sourceReferenceId: String?,
    val sourceReferenceType: String?,
    val deliveryType: DeliveryOrderType,
    val priority: DeliveryPriority,
    val status: DeliveryOrderStatus,
    val requestedDeliveryDate: Long,
    val notes: String?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    init {
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderNo.isNotBlank()) { "Delivery Order Number cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Created At timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated At cannot be before Created At." }
    }
}
