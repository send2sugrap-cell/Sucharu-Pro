package com.sucharu.sucharupro.domain.model.jobcosting

import java.math.BigDecimal

/**
 * BI-02-A Cost Variance Classifications.
 */
enum class JobCostVarianceClassification {
    ON_TARGET,
    COST_OVER_ESTIMATE,
    COST_UNDER_ESTIMATE,
    NO_ESTIMATE,
    NO_ACTUAL_COST
}

/**
 * Detailed Printing Cost Component Breakdown for a Job.
 */
data class PrintingCostComponentBreakdown(
    val materialCost: BigDecimal = BigDecimal.ZERO,
    val pressLaborCost: BigDecimal = BigDecimal.ZERO,
    val machineOperationCost: BigDecimal = BigDecimal.ZERO,
    val finishingLaminationCost: BigDecimal = BigDecimal.ZERO,
    val packagingCost: BigDecimal = BigDecimal.ZERO,
    val scrapWasteCost: BigDecimal = BigDecimal.ZERO,
    val reworkCost: BigDecimal = BigDecimal.ZERO,
    val overheadCost: BigDecimal = BigDecimal.ZERO
) {
    val totalComponentCost: BigDecimal
        get() = materialCost + pressLaborCost + machineOperationCost + finishingLaminationCost + packagingCost + scrapWasteCost + reworkCost + overheadCost
}

/**
 * Canonical Printing Job Cost Breakdown Summary Model.
 */
data class JobCostBreakdownSummary(
    val jobId: String,
    val orderId: String,
    val customerName: String,
    val productName: String,
    val orderQuantity: BigDecimal,

    val estimatedTotalCost: BigDecimal,
    val actualTotalCost: BigDecimal,
    val costVariance: BigDecimal,
    val costVariancePercentage: BigDecimal,
    val varianceClassification: JobCostVarianceClassification,

    val sellingPriceSnapshot: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal,

    val costComponents: PrintingCostComponentBreakdown,
    val calculatedAt: String
)

/**
 * BI-02-A Master Printing Job Costing Intelligence Summary.
 */
data class JobCostIntelligenceSummary(
    val totalJobsCount: Int,
    val onTargetJobsCount: Int,
    val overEstimateJobsCount: Int,
    val underEstimateJobsCount: Int,
    val totalEstimatedCostSum: BigDecimal,
    val totalActualCostSum: BigDecimal,
    val netCostVarianceSum: BigDecimal,
    val totalSellingPriceSnapshotSum: BigDecimal,
    val averageGrossMarginPercentage: BigDecimal,
    val jobBreakdowns: List<JobCostBreakdownSummary> = emptyList(),
    val generatedAt: String
)
