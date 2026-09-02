package com.sucharu.sucharupro.domain.model.delivery.reconciliation

import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus

/**
 * Aggregate root for Delivery Reconciliation & Settlement (Module 08 Step 09).
 */
data class DeliveryReconciliation(
    val reconciliationId: String,
    val projectId: String,
    val deliveryOrderId: String,
    val deliveryChallanId: String? = null,
    val deliveryShipmentId: String? = null,
    val proofId: String? = null,
    val orderedQuantity: Double = 0.0,
    val challanedQuantity: Double = 0.0,
    val dispatchedQuantity: Double = 0.0,
    val deliveredQuantity: Double = 0.0,
    val acceptedPodQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val returnedQuantity: Double = 0.0,
    val outstandingQuantity: Double = 0.0,
    val discrepancyQuantity: Double = 0.0,
    val reconciliationStatus: DeliveryReconciliationStatus = DeliveryReconciliationStatus.OPEN,
    val settlementStatus: DeliverySettlementStatus = DeliverySettlementStatus.OPEN,
    val reconciliationReason: String? = null,
    val resolutionNotes: String? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val closedBy: String? = null,
    val closedAt: Long? = null,
    val version: Int = 1,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(reconciliationId.isNotBlank()) { "Reconciliation ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(orderedQuantity >= 0) { "Ordered quantity cannot be negative." }
        require(challanedQuantity >= 0) { "Challaned quantity cannot be negative." }
        require(dispatchedQuantity >= 0) { "Dispatched quantity cannot be negative." }
        require(deliveredQuantity >= 0) { "Delivered quantity cannot be negative." }
        require(acceptedPodQuantity >= 0) { "Accepted POD quantity cannot be negative." }
        require(rejectedQuantity >= 0) { "Rejected quantity cannot be negative." }
        require(returnedQuantity >= 0) { "Returned quantity cannot be negative." }
        require(outstandingQuantity >= 0) { "Outstanding quantity cannot be negative." }
        require(discrepancyQuantity >= 0) { "Discrepancy quantity cannot be negative." }
        require(version >= 1) { "Version must be >= 1." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation." }
    }

    val isReconciled: Boolean
        get() = reconciliationStatus == DeliveryReconciliationStatus.RECONCILED ||
                reconciliationStatus == DeliveryReconciliationStatus.RESOLVED ||
                reconciliationStatus == DeliveryReconciliationStatus.CLOSED

    val isOperationallySettled: Boolean
        get() = settlementStatus == DeliverySettlementStatus.SETTLED ||
                (deliveredQuantity >= orderedQuantity && outstandingQuantity <= 0.0 && discrepancyQuantity <= 0.0 && acceptedPodQuantity >= deliveredQuantity)
}
