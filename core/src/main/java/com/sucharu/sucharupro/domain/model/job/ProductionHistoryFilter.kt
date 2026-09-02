package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Filter for historical completion status.
 */
enum class CompletionFilter(val label: String) {
    ALL("All"),
    COMPLETED("Completed"),
    INCOMPLETE("Incomplete")
}

/**
 * Logical date range options for historical queries.
 */
enum class ProductionDateRangeFilter(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

/**
 * Sorting criteria for production history records.
 */
enum class ProductionHistorySortBy(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    DURATION_DESC("Longest Duration"),
    DURATION_ASC("Shortest Duration"),
    PROGRESS_DESC("Highest Progress"),
    PROGRESS_ASC("Lowest Progress"),
    PRIORITY_DESC("Highest Priority")
}

/**
 * Composable filter and sorting parameters for querying production history.
 */
data class ProductionHistoryFilter(
    val status: ProductionJobStatus? = null,
    val priority: OrderPriority? = null,
    val completion: CompletionFilter = CompletionFilter.ALL,
    val stageType: ProductionStageType? = null,
    val dateRange: ProductionDateRangeFilter = ProductionDateRangeFilter.ALL_TIME,
    val sortBy: ProductionHistorySortBy = ProductionHistorySortBy.DATE_DESC
)
