package com.sucharu.sucharupro.domain.model.delivery.partial

/**
 * Line item entity for product-level settlement quantification (Module 08 Step 06).
 */
data class DeliveryPartialSettlementLine(
    val settlementLineId: String,
    val projectId: String,
    val settlementId: String,
    val deliveryOrderLineId: String,
    val productId: String,
    val orderedQuantity: Double,
    val allocatedQuantity: Double = 0.0,
    val dispatchedQuantity: Double = 0.0,
    val deliveredQuantity: Double = 0.0,
    val shortQuantity: Double = 0.0,
    val excessQuantity: Double = 0.0,
    val returnedQuantity: Double = 0.0,
    val replacementQuantity: Double = 0.0,
    val pendingQuantity: Double = orderedQuantity,
    val status: DeliverySettlementStatus = DeliverySettlementStatus.OPEN,
    val createdAt: Long,
    val updatedAt: Long
) {
    init {
        require(settlementLineId.isNotBlank()) { "Settlement Line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(settlementId.isNotBlank()) { "Settlement ID cannot be blank." }
        require(deliveryOrderLineId.isNotBlank()) { "Delivery Order Line ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(orderedQuantity >= 0) { "Ordered quantity cannot be negative." }
        require(allocatedQuantity >= 0) { "Allocated quantity cannot be negative." }
        require(dispatchedQuantity >= 0) { "Dispatched quantity cannot be negative." }
        require(deliveredQuantity >= 0) { "Delivered quantity cannot be negative." }
        require(shortQuantity >= 0) { "Short quantity cannot be negative." }
        require(excessQuantity >= 0) { "Excess quantity cannot be negative." }
        require(returnedQuantity >= 0) { "Returned quantity cannot be negative." }
        require(replacementQuantity >= 0) { "Replacement quantity cannot be negative." }
        require(pendingQuantity >= 0) { "Pending quantity cannot be negative." }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp must be >= created timestamp." }
    }

    val isFullyDelivered: Boolean
        get() = deliveredQuantity >= orderedQuantity && pendingQuantity <= 0.0

    val isPartiallyDelivered: Boolean
        get() = deliveredQuantity > 0.0 && deliveredQuantity < orderedQuantity
}
