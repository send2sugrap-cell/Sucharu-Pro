package com.sucharu.sucharupro.data.api.model.intelligence

import kotlinx.serialization.Serializable

@Serializable
data class DecisionKpiItemDto(
    val kpiCode: String,
    val title: String,
    val currentValue: String,
    val baselineValue: String,
    val trendPercentage: Double,
    val trendDirection: String = "UPWARD",
    val statusMessage: String
)

@Serializable
data class DecisionExceptionItemDto(
    val exceptionId: String,
    val category: String,
    val severity: String = "NORMAL_ATTENTION",
    val title: String,
    val description: String,
    val affectedRecordId: String? = null,
    val targetRoute: String? = null
)

@Serializable
data class DecisionIntelligenceSummaryDto(
    val totalRevenueYtd: String,
    val activeOrdersCount: Int,
    val quotationConversionRatePercentage: String,
    val activeProductionJobsCount: Int,
    val totalOutstandingReceivable: String,
    val overdueReceivableAmount: String,
    val averageGrossMarginPercentage: String,
    val totalProcurementCommitment: String,

    val kpiList: List<DecisionKpiItemDto> = emptyList(),
    val exceptionQueue: List<DecisionExceptionItemDto> = emptyList(),

    val isShadowAnalyticsDatabaseCreated: Boolean = false,
    val generatedAt: String
)
