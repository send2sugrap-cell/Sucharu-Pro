package com.sucharu.sucharupro.domain.model.jobcosting

import java.math.BigDecimal

/**
 * Single Cost Component Variance Item for BI-02-B.
 */
data class CostComponentVarianceItem(
    val category: CostCategory,
    val componentName: String,
    val estimatedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val varianceAmount: BigDecimal,
    val variancePercentage: BigDecimal,
    val classification: JobCostVarianceClassification,
    val leakageNotes: String? = null
)

/**
 * Cost Leakage Analysis Summary across production factors.
 */
data class CostLeakageSummary(
    val primaryLeakageSource: String,
    val materialPriceQuantityLeakage: BigDecimal = BigDecimal.ZERO,
    val laborEfficiencyLeakage: BigDecimal = BigDecimal.ZERO,
    val machineDowntimeLeakage: BigDecimal = BigDecimal.ZERO,
    val scrapWasteLeakage: BigDecimal = BigDecimal.ZERO,
    val reworkConversionLeakage: BigDecimal = BigDecimal.ZERO,
    val overheadAllocationLeakage: BigDecimal = BigDecimal.ZERO
)

/**
 * BI-02-B Detailed Component-Level Job Cost & Variance Model.
 */
data class JobCostComponentVarianceDetail(
    val jobId: String,
    val orderId: String,
    val customerName: String,
    val productName: String,
    val orderQuantity: BigDecimal,

    val totalEstimatedCost: BigDecimal,
    val totalActualCost: BigDecimal,
    val netCostVariance: BigDecimal,
    val overallClassification: JobCostVarianceClassification,

    val componentVariances: List<CostComponentVarianceItem> = emptyList(),
    val leakageSummary: CostLeakageSummary,
    val isReconciled: Boolean = true,
    val calculatedAt: String
)
