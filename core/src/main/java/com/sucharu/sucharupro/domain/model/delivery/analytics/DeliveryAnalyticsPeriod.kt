package com.sucharu.sucharupro.domain.model.delivery.analytics

/**
 * Period selector for delivery analytics aggregation.
 */
enum class DeliveryAnalyticsPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}
