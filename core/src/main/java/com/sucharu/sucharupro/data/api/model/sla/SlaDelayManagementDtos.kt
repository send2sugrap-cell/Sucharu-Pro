package com.sucharu.sucharupro.data.api.model.sla

import kotlinx.serialization.Serializable

@Serializable
data class SlaOrderCommitmentDto(
    val commitmentId: String,
    val orderId: String,
    val jobId: String? = null,
    val customerId: String,
    val customerName: String,
    val promisedDeliveryDate: String,
    val plannedProductionCompletionDate: String? = null,
    val actualProductionCompletionDate: String? = null,
    val actualDeliveryDate: String? = null,
    val slaStatus: String = "ON_TRACK",
    val delayDays: Int = 0,
    val currentStage: String = "PRINTING"
)

@Serializable
data class RecordDelayReasonRequestDto(
    val commitmentId: String,
    val orderId: String,
    val delayCategory: String = "PRODUCTION_DELAY",
    val responsibleStage: String = "PRODUCTION",
    val delayDurationDays: Int = 1,
    val rootCauseDescription: String
)

@Serializable
data class SlaDelayRecordDto(
    val delayRecordId: String,
    val commitmentId: String,
    val orderId: String,
    val delayCategory: String,
    val responsibleStage: String = "PRODUCTION",
    val delayDurationDays: Int = 1,
    val rootCauseDescription: String,
    val recordedAt: String,
    val recordedBy: String
)

@Serializable
data class SlaManagementSummaryDto(
    val totalMonitoredOrdersCount: Int,
    val onTrackOrdersCount: Int,
    val atRiskOrdersCount: Int,
    val overdueOrdersCount: Int,
    val completedOnTimeCount: Int,
    val completedLateCount: Int,
    val averageDelayDays: Double,
    val slaOnTimePerformancePercentage: String,
    val exceptionQueue: List<SlaOrderCommitmentDto> = emptyList(),
    val delayRecords: List<SlaDelayRecordDto> = emptyList(),
    val generatedAt: String
)
