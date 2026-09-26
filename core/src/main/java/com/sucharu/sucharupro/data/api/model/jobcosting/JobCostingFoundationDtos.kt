package com.sucharu.sucharupro.data.api.model.jobcosting

import kotlinx.serialization.Serializable

@Serializable
data class PrintingCostComponentBreakdownDto(
    val materialCost: String = "0.00",
    val pressLaborCost: String = "0.00",
    val machineOperationCost: String = "0.00",
    val finishingLaminationCost: String = "0.00",
    val packagingCost: String = "0.00",
    val scrapWasteCost: String = "0.00",
    val reworkCost: String = "0.00",
    val overheadCost: String = "0.00"
)

@Serializable
data class JobCostBreakdownSummaryDto(
    val jobId: String,
    val orderId: String,
    val customerName: String,
    val productName: String,
    val orderQuantity: String,

    val estimatedTotalCost: String,
    val actualTotalCost: String,
    val costVariance: String,
    val costVariancePercentage: String,
    val varianceClassification: String = "ON_TARGET",

    val sellingPriceSnapshot: String,
    val grossProfit: String,
    val grossMarginPercentage: String,

    val costComponents: PrintingCostComponentBreakdownDto,
    val calculatedAt: String
)

@Serializable
data class JobCostIntelligenceSummaryDto(
    val totalJobsCount: Int,
    val onTargetJobsCount: Int,
    val overEstimateJobsCount: Int,
    val underEstimateJobsCount: Int,
    val totalEstimatedCostSum: String,
    val totalActualCostSum: String,
    val netCostVarianceSum: String,
    val totalSellingPriceSnapshotSum: String,
    val averageGrossMarginPercentage: String,
    val jobBreakdowns: List<JobCostBreakdownSummaryDto> = emptyList(),
    val generatedAt: String
)
