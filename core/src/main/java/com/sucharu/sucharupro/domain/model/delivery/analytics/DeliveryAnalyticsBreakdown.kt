package com.sucharu.sucharupro.domain.model.delivery.analytics

import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.pod.DeliveryProofStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus

/**
 * Breakdown of delivery data across multiple business dimensions.
 */
data class DeliveryAnalyticsBreakdown(
    val projectId: String,
    val byOrderStatus: Map<DeliveryOrderStatus, Int> = emptyMap(),
    val byShipmentStatus: Map<DeliveryShipmentStatus, Int> = emptyMap(),
    val byProofStatus: Map<DeliveryProofStatus, Int> = emptyMap(),
    val byReconciliationStatus: Map<DeliveryReconciliationStatus, Int> = emptyMap(),
    val byCustomer: Map<String, CustomerDeliveryMetric> = emptyMap(),
    val byCarrier: Map<String, Int> = emptyMap()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}

/**
 * Per-customer delivery aggregate metric.
 */
data class CustomerDeliveryMetric(
    val customerId: String,
    val totalOrders: Int,
    val deliveredOrders: Int,
    val returnedOrders: Int,
    val orderedQuantity: Double,
    val deliveredQuantity: Double,
    val returnedQuantity: Double
)
