package com.sucharu.sucharupro.domain.model.delivery.analytics

/**
 * Filter criteria for delivery analytics queries.
 */
data class DeliveryAnalyticsFilter(
    val projectId: String,
    val period: DeliveryAnalyticsPeriod = DeliveryAnalyticsPeriod.ALL_TIME,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val customerId: String? = null,
    val deliveryOrderId: String? = null,
    val carrierName: String? = null
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        if (dateFrom != null && dateTo != null) {
            require(dateTo >= dateFrom) { "Date To ($dateTo) cannot be earlier than Date From ($dateFrom)." }
        }
    }
}
