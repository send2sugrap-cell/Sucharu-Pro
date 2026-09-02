package com.sucharu.sucharupro.domain.model.delivery.partial

/**
 * High-level KPI metrics summary for Delivery Partial Settlements (Module 08 Step 06).
 */
data class DeliveryPartialSettlementSummary(
    val totalSettlements: Int = 0,
    val openCount: Int = 0,
    val partiallyDeliveredCount: Int = 0,
    val fullyDeliveredCount: Int = 0,
    val partiallyReturnedCount: Int = 0,
    val settlementPendingCount: Int = 0,
    val settledCount: Int = 0,
    val disputedCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalOrderedQuantity: Double = 0.0,
    val totalDeliveredQuantity: Double = 0.0,
    val totalPendingQuantity: Double = 0.0,
    val totalSplitDispatches: Int = 0
) {
    val overallDeliveryCompletionPercentage: Double
        get() = if (totalOrderedQuantity > 0.0) {
            ((totalDeliveredQuantity / totalOrderedQuantity) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }
}
