package com.sucharu.sucharupro.data.api.model.ai.ops

import kotlinx.serialization.Serializable

@Serializable
data class StructuredOpsObservationDto(
    val observationId: String,
    val category: String,
    val factSummary: String,
    val interpretation: String,
    val recommendation: String,
    val targetEntityId: String? = null
)

@Serializable
data class ProductionOpsDailyBriefDto(
    val totalActiveJobsCount: Int,
    val delayedJobsCount: Int,
    val slaRiskJobsCount: Int,
    val readyShipmentsCount: Int,
    val qcExceptionsCount: Int,
    val vendorBlockersCount: Int,
    val observations: List<StructuredOpsObservationDto> = emptyList(),
    val isCanonicalPipelinePreserved: Boolean = true,
    val generatedAt: String
)
