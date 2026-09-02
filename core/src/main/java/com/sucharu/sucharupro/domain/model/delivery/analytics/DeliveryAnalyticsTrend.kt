package com.sucharu.sucharupro.domain.model.delivery.analytics

/**
 * Single data point within an analytics time series trend.
 */
data class DeliveryAnalyticsTrendPoint(
    val timestamp: Long,
    val dateLabel: String,
    val orderCount: Int,
    val dispatchedCount: Int,
    val deliveredCount: Int,
    val acceptedPodCount: Int,
    val returnedCount: Int,
    val discrepancyCount: Int,
    val deliveredQuantity: Double
) {
    init {
        require(timestamp > 0) { "Timestamp must be positive." }
        require(dateLabel.isNotBlank()) { "Date label cannot be blank." }
        require(orderCount >= 0) { "Order count cannot be negative." }
        require(dispatchedCount >= 0) { "Dispatched count cannot be negative." }
        require(deliveredCount >= 0) { "Delivered count cannot be negative." }
        require(acceptedPodCount >= 0) { "Accepted POD count cannot be negative." }
        require(returnedCount >= 0) { "Returned count cannot be negative." }
        require(discrepancyCount >= 0) { "Discrepancy count cannot be negative." }
        require(deliveredQuantity >= 0.0) { "Delivered quantity cannot be negative." }
    }
}

/**
 * Aggregated delivery trends over time.
 */
data class DeliveryAnalyticsTrend(
    val projectId: String,
    val period: DeliveryAnalyticsPeriod,
    val points: List<DeliveryAnalyticsTrendPoint> = emptyList()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }
}
