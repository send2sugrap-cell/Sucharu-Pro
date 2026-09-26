package com.sucharu.sucharupro.domain.model.jobcosting

import java.math.BigDecimal

/**
 * BI-02-C Cost Intelligence Confidence States.
 */
enum class CostIntelligenceConfidence {
    SUFFICIENT_DATA,
    LIMITED_DATA,
    INSUFFICIENT_DATA,
    DATA_RECONCILIATION_REQUIRED
}

/**
 * Historical Actual Cost Benchmark Model derived from completed jobs.
 */
data class HistoricalCostBenchmark(
    val comparableJobsCount: Int,
    val avgActualUnitCost: BigDecimal,
    val minActualUnitCost: BigDecimal,
    val maxActualUnitCost: BigDecimal,
    val avgCostVariancePercentage: BigDecimal,
    val avgGrossMarginPercentage: BigDecimal
)

/**
 * BI-02-C Quotation Cost Intelligence & Commercial Decision Support Model.
 */
data class QuotationCostIntelligence(
    val quotationId: String,
    val productId: String,
    val productName: String,
    val requestedQuantity: BigDecimal,

    val currentQuotationEstimatedCost: BigDecimal,
    val currentQuotationEstimatedUnitCost: BigDecimal,
    val currentQuotationSellingPrice: BigDecimal, // Form 04 OrderPriceSnapshot / Selling Price

    val historicalBenchmark: HistoricalCostBenchmark?,
    val confidenceState: CostIntelligenceConfidence,

    val costVarianceAlert: String? = null,
    val recommendationMessage: String,
    val isAutomaticPriceMutationApplied: Boolean = false, // ALWAYS FALSE! (Critical Invariant)
    val calculatedAt: String
)
