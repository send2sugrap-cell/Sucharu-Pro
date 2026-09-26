package com.sucharu.sucharupro.data.api.model.jobcosting

import kotlinx.serialization.Serializable

@Serializable
data class HistoricalCostBenchmarkDto(
    val comparableJobsCount: Int,
    val avgActualUnitCost: String,
    val minActualUnitCost: String,
    val maxActualUnitCost: String,
    val avgCostVariancePercentage: String,
    val avgGrossMarginPercentage: String
)

@Serializable
data class QuotationCostIntelligenceDto(
    val quotationId: String,
    val productId: String,
    val productName: String,
    val requestedQuantity: String,

    val currentQuotationEstimatedCost: String,
    val currentQuotationEstimatedUnitCost: String,
    val currentQuotationSellingPrice: String,

    val historicalBenchmark: HistoricalCostBenchmarkDto? = null,
    val confidenceState: String = "SUFFICIENT_DATA",

    val costVarianceAlert: String? = null,
    val recommendationMessage: String,
    val isAutomaticPriceMutationApplied: Boolean = false,
    val calculatedAt: String
)
