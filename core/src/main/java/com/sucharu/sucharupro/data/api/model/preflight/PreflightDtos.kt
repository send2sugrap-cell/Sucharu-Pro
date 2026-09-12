package com.sucharu.sucharupro.data.api.model.preflight

import com.sucharu.sucharupro.domain.preflight.*

/**
 * REST API DTOs for Automated Proofing & Preflight Engine & Finding Governance.
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
    val status: PreflightFindingStatus,
    val acknowledgedBy: String?,
    val acknowledgedAt: Long?,
    val resolvedAt: Long?,
    val resolvedBy: String?,
    val waiverReason: String?,
    val waivedBy: String?,
    val waivedAt: Long?,
    val revalidationRunId: String?,
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
            status = domain.status,
            acknowledgedBy = domain.acknowledgedBy,
            acknowledgedAt = domain.acknowledgedAt,
            resolvedAt = domain.resolvedAt,
            resolvedBy = domain.resolvedBy,
            waiverReason = domain.waiverReason,
            waivedBy = domain.waivedBy,
            waivedAt = domain.waivedAt,
            revalidationRunId = domain.revalidationRunId,
            createdAt = domain.createdAt
        )
    }
}

data class SubmitCorrectionRequestDto(
    val correctionType: PreflightCorrectionType = PreflightCorrectionType.OTHER,
    val description: String,
    val artworkVersionId: String? = null
)

data class PreflightFindingCorrectionResponseDto(
    val correctionId: String,
    val tenantId: String,
    val findingId: String,
    val preflightRunId: String,
    val correctionType: PreflightCorrectionType,
    val description: String,
    val artworkVersionId: String?,
    val submittedBy: String,
    val submittedAt: Long,
    val revalidationRunId: String?
) {
    companion object {
        fun fromDomain(domain: PreflightFindingCorrection): PreflightFindingCorrectionResponseDto = PreflightFindingCorrectionResponseDto(
            correctionId = domain.correctionId,
            tenantId = domain.tenantId,
            findingId = domain.findingId,
            preflightRunId = domain.preflightRunId,
            correctionType = domain.correctionType,
            description = domain.description,
            artworkVersionId = domain.artworkVersionId,
            submittedBy = domain.submittedBy,
            submittedAt = domain.submittedAt,
            revalidationRunId = domain.revalidationRunId
        )
    }
}

data class WaiveFindingRequestDto(
    val waiverReason: String
)

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

data class EvaluateProductionReadinessRequestDto(
    val artworkId: String,
    val artworkVersionId: String? = null,
    val proofId: String? = null,
    val proofVersionId: String? = null,
    val jobId: String? = null,
    val preflightRunId: String? = null
)

data class PreflightProductionReadinessResponseDto(
    val readinessId: String,
    val tenantId: String,
    val artworkId: String,
    val artworkVersionId: String?,
    val proofId: String?,
    val proofVersionId: String?,
    val jobId: String?,
    val preflightRunId: String?,
    val decision: ProductionReadinessDecision,
    val blockingFindingCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val blockingReasons: List<String>,
    val evaluatedBy: String,
    val evaluatedAt: Long
) {
    companion object {
        fun fromDomain(domain: PreflightProductionReadiness): PreflightProductionReadinessResponseDto = PreflightProductionReadinessResponseDto(
            readinessId = domain.readinessId,
            tenantId = domain.tenantId,
            artworkId = domain.artworkId,
            artworkVersionId = domain.artworkVersionId,
            proofId = domain.proofId,
            proofVersionId = domain.proofVersionId,
            jobId = domain.jobId,
            preflightRunId = domain.preflightRunId,
            decision = domain.decision,
            blockingFindingCount = domain.blockingFindingCount,
            warningCount = domain.warningCount,
            infoCount = domain.infoCount,
            blockingReasons = domain.blockingReasons,
            evaluatedBy = domain.evaluatedBy,
            evaluatedAt = domain.evaluatedAt
        )
    }
}
