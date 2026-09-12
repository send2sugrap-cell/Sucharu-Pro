package com.sucharu.sucharupro.data.api.model.preflight

import com.sucharu.sucharupro.domain.preflight.*

/**
 * REST API DTOs for Automated Proofing & Preflight Engine.
 */
data class StartPreflightRequestDto(
    val artworkId: String,
    val jobId: String? = null,
    val artworkVersionId: String? = null,
    val proofId: String? = null,
    val idempotencyKey: String? = null,
    val orderSpecificationMap: Map<String, Any> = emptyMap(),
    val artworkMetadataMap: Map<String, Any> = emptyMap()
)

data class PreflightRunResponseDto(
    val preflightRunId: String,
    val tenantId: String,
    val jobId: String?,
    val artworkId: String,
    val artworkVersionId: String?,
    val proofId: String?,
    val status: PreflightRunStatus,
    val overallResult: PreflightOverallResult,
    val engineVersion: String,
    val idempotencyKey: String?,
    val requestedBy: String,
    val startedAt: Long?,
    val completedAt: Long?,
    val summary: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromDomain(domain: PreflightRun): PreflightRunResponseDto = PreflightRunResponseDto(
            preflightRunId = domain.preflightRunId,
            tenantId = domain.tenantId,
            jobId = domain.jobId,
            artworkId = domain.artworkId,
            artworkVersionId = domain.artworkVersionId,
            proofId = domain.proofId,
            status = domain.status,
            overallResult = domain.overallResult,
            engineVersion = domain.engineVersion,
            idempotencyKey = domain.idempotencyKey,
            requestedBy = domain.requestedBy,
            startedAt = domain.startedAt,
            completedAt = domain.completedAt,
            summary = domain.summary,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}

data class PreflightFindingResponseDto(
    val findingId: String,
    val tenantId: String,
    val preflightRunId: String,
    val executionId: String?,
    val ruleCode: String,
    val category: PreflightRuleCategory,
    val severity: PreflightRuleSeverity,
    val message: String,
    val expectedValue: String?,
    val actualValue: String?,
    val locationContext: String?,
    val createdAt: Long
) {
    companion object {
        fun fromDomain(domain: PreflightFinding): PreflightFindingResponseDto = PreflightFindingResponseDto(
            findingId = domain.findingId,
            tenantId = domain.tenantId,
            preflightRunId = domain.preflightRunId,
            executionId = domain.executionId,
            ruleCode = domain.ruleCode,
            category = domain.category,
            severity = domain.severity,
            message = domain.message,
            expectedValue = domain.expectedValue,
            actualValue = domain.actualValue,
            locationContext = domain.locationContext,
            createdAt = domain.createdAt
        )
    }
}

data class PreflightRuleExecutionResponseDto(
    val executionId: String,
    val tenantId: String,
    val preflightRunId: String,
    val ruleId: String,
    val ruleCode: String,
    val result: PreflightExecutionResult,
    val startedAt: Long,
    val completedAt: Long,
    val errorMessage: String?
) {
    companion object {
        fun fromDomain(domain: PreflightRuleExecution): PreflightRuleExecutionResponseDto = PreflightRuleExecutionResponseDto(
            executionId = domain.executionId,
            tenantId = domain.tenantId,
            preflightRunId = domain.preflightRunId,
            ruleId = domain.ruleId,
            ruleCode = domain.ruleCode,
            result = domain.result,
            startedAt = domain.startedAt,
            completedAt = domain.completedAt,
            errorMessage = domain.errorMessage
        )
    }
}
