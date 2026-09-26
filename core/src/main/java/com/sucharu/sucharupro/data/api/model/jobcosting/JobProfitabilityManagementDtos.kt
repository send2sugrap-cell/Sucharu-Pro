package com.sucharu.sucharupro.data.api.model.jobcosting

import kotlinx.serialization.Serializable

@Serializable
data class JobProfitabilityManagementItemDto(
    val jobId: String,
    val orderId: String,
    val customerName: String,
    val productName: String,
    val orderQuantity: String,

    val sellingPriceSnapshot: String,
    val estimatedTotalCost: String,
    val actualTotalCost: String,

    val grossProfit: String,
    val actualGrossMarginPercentage: String,
    val estimatedGrossMarginPercentage: String,
    val grossMarginPercentageDelta: String,

    val costLeakageDriver: String,
    val profitabilityStatus: String = "PROFITABLE",
    val isFullyReconciled: Boolean = true
)

@Serializable
data class JobProfitabilityManagementSummaryDto(
    val totalJobsEvaluated: Int,
    val profitableJobsCount: Int,
    val breakEvenJobsCount: Int,
    val lossJobsCount: Int,

    val totalSellingRevenueSum: String,
    val totalEstimatedCostSum: String,
    val totalActualCostSum: String,
    val totalGrossProfitSum: String,
    val averageGrossMarginPercentage: String,
    val averageEstimateAccuracyPercentage: String,

    val jobProfitabilityItems: List<JobProfitabilityManagementItemDto> = emptyList(),
    val generatedAt: String
)
