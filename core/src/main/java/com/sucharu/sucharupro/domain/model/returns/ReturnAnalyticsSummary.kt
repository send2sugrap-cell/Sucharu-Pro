package com.sucharu.sucharupro.domain.model.returns

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * High-level executive KPI summary and metrics for Return Management (Module 11 Step 06).
 */
data class ReturnAnalyticsSummary(
    val projectId: String,
    val period: ReturnAnalyticsPeriod,
    val totalReturns: Int,
    val returnRate: Double,
    val openReturns: Int,
    val processedReturns: Int,
    val settledReturns: Int,
    val totalRequestedQuantity: Int,
    val totalAcceptedQuantity: Int,
    val totalRejectedQuantity: Int,
    val totalSettledValue: Money,
    val averageTurnaroundDays: Double,
    val generatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(totalReturns >= 0) { "Total returns cannot be negative." }
        require(returnRate >= 0.0) { "Return rate cannot be negative." }
        require(openReturns >= 0) { "Open returns cannot be negative." }
        require(processedReturns >= 0) { "Processed returns cannot be negative." }
        require(settledReturns >= 0) { "Settled returns cannot be negative." }
        require(totalRequestedQuantity >= 0) { "Total requested quantity cannot be negative." }
        require(totalAcceptedQuantity >= 0) { "Total accepted quantity cannot be negative." }
        require(totalRejectedQuantity >= 0) { "Total rejected quantity cannot be negative." }
        require(!totalSettledValue.isNegative()) { "Total settled value cannot be negative." }
        require(averageTurnaroundDays >= 0.0) { "Average turnaround days cannot be negative." }
        require(generatedAt > 0) { "Generated at timestamp must be positive." }
    }
}
