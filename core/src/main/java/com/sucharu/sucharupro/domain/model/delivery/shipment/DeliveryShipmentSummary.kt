package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Operational metric summary for Delivery Shipments (Module 08 Step 05).
 */
data class DeliveryShipmentSummary(
    val totalShipments: Int = 0,
    val draftCount: Int = 0,
    val readyCount: Int = 0,
    val dispatchedCount: Int = 0,
    val inTransitCount: Int = 0,
    val outForDeliveryCount: Int = 0,
    val deliveryAttemptedCount: Int = 0,
    val delayedCount: Int = 0,
    val onHoldCount: Int = 0,
    val deliveredCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalAttempts: Int = 0
) {
    val activeCount: Int
        get() = dispatchedCount + inTransitCount + outForDeliveryCount + deliveryAttemptedCount + delayedCount + onHoldCount
}
