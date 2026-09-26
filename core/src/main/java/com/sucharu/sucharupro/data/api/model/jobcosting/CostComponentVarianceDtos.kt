package com.sucharu.sucharupro.data.api.model.jobcosting

import kotlinx.serialization.Serializable

@Serializable
data class CostComponentVarianceItemDto(
    val category: String,
    val componentName: String,
    val estimatedAmount: String,
    val actualAmount: String,
    val varianceAmount: String,
    val variancePercentage: String,
    val classification: String = "ON_TARGET",
    val leakageNotes: String? = null
)

@Serializable
data class CostLeakageSummaryDto(
    val primaryLeakageSource: String,
    val materialPriceQuantityLeakage: String = "0.00",
    val laborEfficiencyLeakage: String = "0.00",
    val machineDowntimeLeakage: String = "0.00",
    val scrapWasteLeakage: String = "0.00",
    val reworkConversionLeakage: String = "0.00",
    val overheadAllocationLeakage: String = "0.00"
)

@Serializable
data class JobCostComponentVarianceDetailDto(
    val jobId: String,
    val orderId: String,
    val customerName: String,
    val productName: String,
    val orderQuantity: String,

    val totalEstimatedCost: String,
    val totalActualCost: String,
    val netCostVariance: String,
    val overallClassification: String = "ON_TARGET",

    val componentVariances: List<CostComponentVarianceItemDto> = emptyList(),
    val leakageSummary: CostLeakageSummaryDto,
    val isReconciled: Boolean = true,
    val calculatedAt: String
)
