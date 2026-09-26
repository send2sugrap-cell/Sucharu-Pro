package com.sucharu.sucharupro.domain.model.jobcosting

import java.math.BigDecimal

/**
 * BI-02-D Job Profitability Health Status.
 */
enum class JobProfitabilityHealthStatus {
    PROFITABLE,
    BREAK_EVEN,
    LOSS,
    DATA_INSUFFICIENT
}

/**
 * Single Job Profitability Management Item.
 */
data class JobProfitabilityManagementItem(
    val jobId: String,
    val orderId: String,
    val customerName: String,
    val productName: String,
    val orderQuantity: BigDecimal,

    val sellingPriceSnapshot: BigDecimal, // Form 04 OrderPriceSnapshot
    val estimatedTotalCost: BigDecimal,
    val actualTotalCost: BigDecimal,

    val grossProfit: BigDecimal,
    val actualGrossMarginPercentage: BigDecimal,
    val estimatedGrossMarginPercentage: BigDecimal,
    val grossMarginPercentageDelta: BigDecimal,

    val costLeakageDriver: String,
    val profitabilityStatus: JobProfitabilityHealthStatus,
    val isFullyReconciled: Boolean = true
)

/**
 * BI-02-D Master Job Profitability & Management Intelligence Model.
 */
data class JobProfitabilityManagementSummary(
    val totalJobsEvaluated: Int,
    val profitableJobsCount: Int,
    val breakEvenJobsCount: Int,
    val lossJobsCount: Int,

    val totalSellingRevenueSum: BigDecimal,
    val totalEstimatedCostSum: BigDecimal,
    val totalActualCostSum: BigDecimal,
    val totalGrossProfitSum: BigDecimal,
    val averageGrossMarginPercentage: BigDecimal,
    val averageEstimateAccuracyPercentage: BigDecimal,

    val jobProfitabilityItems: List<JobProfitabilityManagementItem> = emptyList(),
    val generatedAt: String
)
