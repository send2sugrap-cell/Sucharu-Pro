package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Line item entity for products in a Delivery Return (Module 08 Step 07).
 */
data class DeliveryReturnLine(
    val returnLineId: String,
    val returnId: String,
    val projectId: String,
    val deliveryOrderLineId: String,
    val challanLineId: String? = null,
    val dispatchExecutionLineId: String? = null,
    val productId: String,
    val batchId: String? = null,
    val lotId: String? = null,
    val warehouseId: String? = null,
    val locationId: String? = null,
    val returnedQuantity: Double,
    val receivedQuantity: Double = 0.0,
    val acceptedQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val restockedQuantity: Double = 0.0,
    val condition: DeliveryReturnLineCondition = DeliveryReturnLineCondition.UNKNOWN,
    val disposition: DeliveryReturnDisposition = DeliveryReturnDisposition.PENDING_DECISION,
    val isRestocked: Boolean = false,
    val restockMovementId: String? = null,
    val restockedAt: Long? = null,
    val notes: String? = null,
    val inspectionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(returnLineId.isNotBlank()) { "Return Line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderLineId.isNotBlank()) { "Delivery Order Line ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(returnedQuantity > 0) { "Returned quantity must be strictly positive (> 0)." }
        require(receivedQuantity >= 0) { "Received quantity cannot be negative." }
        require(acceptedQuantity >= 0) { "Accepted quantity cannot be negative." }
        require(rejectedQuantity >= 0) { "Rejected quantity cannot be negative." }
        require(restockedQuantity >= 0) { "Restocked quantity cannot be negative." }
        require(restockedQuantity <= acceptedQuantity + 0.001) { "Restocked quantity cannot exceed accepted quantity." }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation." }
    }

    val isInspectionComplete: Boolean
        get() = condition != DeliveryReturnLineCondition.UNKNOWN && (acceptedQuantity > 0 || rejectedQuantity > 0 || receivedQuantity > 0)

    val isDispositionSet: Boolean
        get() = disposition != DeliveryReturnDisposition.PENDING_DECISION
}
