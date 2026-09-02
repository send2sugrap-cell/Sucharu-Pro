package com.sucharu.sucharupro.domain.model.delivery.reconciliation

/**
 * Line-level reconciliation tracking for an individual DeliveryOrder line (Module 08 Step 09).
 */
data class DeliveryReconciliationItem(
    val reconciliationItemId: String,
    val reconciliationId: String,
    val projectId: String,
    val deliveryOrderLineId: String,
    val productId: String,
    val orderedQuantity: Double = 0.0,
    val challanedQuantity: Double = 0.0,
    val dispatchedQuantity: Double = 0.0,
    val deliveredQuantity: Double = 0.0,
    val acceptedPodQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val returnedQuantity: Double = 0.0,
    val outstandingQuantity: Double = 0.0,
    val discrepancyQuantity: Double = 0.0,
    val status: DeliveryReconciliationItemStatus = DeliveryReconciliationItemStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(reconciliationItemId.isNotBlank()) { "Reconciliation Item ID cannot be blank." }
        require(reconciliationId.isNotBlank()) { "Reconciliation ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderLineId.isNotBlank()) { "Delivery Order Line ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(orderedQuantity >= 0) { "Ordered quantity cannot be negative." }
        require(challanedQuantity >= 0) { "Challaned quantity cannot be negative." }
        require(dispatchedQuantity >= 0) { "Dispatched quantity cannot be negative." }
        require(deliveredQuantity >= 0) { "Delivered quantity cannot be negative." }
        require(acceptedPodQuantity >= 0) { "Accepted POD quantity cannot be negative." }
        require(rejectedQuantity >= 0) { "Rejected quantity cannot be negative." }
        require(returnedQuantity >= 0) { "Returned quantity cannot be negative." }
        require(outstandingQuantity >= 0) { "Outstanding quantity cannot be negative." }
        require(discrepancyQuantity >= 0) { "Discrepancy quantity cannot be negative." }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation." }
    }

    val isReconciled: Boolean
        get() = status == DeliveryReconciliationItemStatus.RECONCILED || status == DeliveryReconciliationItemStatus.MATCHED
}
