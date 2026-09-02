package com.sucharu.sucharupro.domain.model.delivery

/**
 * Represents a single item line within a Delivery Order.
 *
 * @param lineId Unique identifier for the line.
 * @param deliveryOrderId Reference to the parent Delivery Order.
 * @param projectId The project context.
 * @param productId Reference to the product being delivered.
 * @param requestedQuantity The quantity requested for delivery.
 * @param notes Optional line-level remarks.
 */
data class DeliveryOrderLine(
    val lineId: String,
    val deliveryOrderId: String,
    val projectId: String,
    val productId: String,
    val requestedQuantity: Double,
    val notes: String?
) {
    init {
        require(lineId.isNotBlank()) { "Line ID cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(requestedQuantity > 0) { "Requested Quantity must be greater than zero." }
    }
}
