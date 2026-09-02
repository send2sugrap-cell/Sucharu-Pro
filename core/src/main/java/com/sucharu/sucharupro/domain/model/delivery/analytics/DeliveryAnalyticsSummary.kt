package com.sucharu.sucharupro.domain.model.delivery.analytics

/**
 * High-level key performance metrics for delivery operations.
 */
data class DeliveryAnalyticsSummary(
    val projectId: String,
    val totalDeliveryOrders: Int = 0,
    val totalChallans: Int = 0,
    val totalDispatches: Int = 0,
    val totalShipments: Int = 0,
    val totalDelivered: Int = 0,
    val totalPartiallyDelivered: Int = 0,
    val totalReturned: Int = 0,
    val totalRejected: Int = 0,
    val totalAcceptedPod: Int = 0,
    val totalPendingPod: Int = 0,
    val totalReconciled: Int = 0,
    val totalDiscrepancies: Int = 0,
    val totalOrderedQuantity: Double = 0.0,
    val totalDispatchedQuantity: Double = 0.0,
    val totalDeliveredQuantity: Double = 0.0,
    val totalReturnedQuantity: Double = 0.0,
    val totalOutstandingQuantity: Double = 0.0,
    val totalDiscrepancyQuantity: Double = 0.0,
    val deliverySuccessRate: Double = 0.0,
    val podAcceptanceRate: Double = 0.0,
    val returnRate: Double = 0.0,
    val discrepancyRate: Double = 0.0,
    val generatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(totalDeliveryOrders >= 0) { "Total delivery orders cannot be negative." }
        require(totalDelivered >= 0) { "Total delivered cannot be negative." }
        require(totalOrderedQuantity >= 0.0) { "Total ordered quantity cannot be negative." }
        require(totalDeliveredQuantity >= 0.0) { "Total delivered quantity cannot be negative." }
        require(deliverySuccessRate in 0.0..100.0) { "Delivery success rate must be between 0 and 100." }
        require(podAcceptanceRate in 0.0..100.0) { "POD acceptance rate must be between 0 and 100." }
        require(returnRate in 0.0..100.0) { "Return rate must be between 0 and 100." }
        require(discrepancyRate in 0.0..100.0) { "Discrepancy rate must be between 0 and 100." }
    }
}
