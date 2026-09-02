package com.sucharu.sucharupro.data.api.model.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import java.math.BigDecimal

/**
 * DTOs for Prepress Orchestration Master Plans.
 * Module 18 Step 06.
 */

data class GeneratePrepressOrchestrationRequestDto(
    val planName: String? = null,
    val jobId: String? = null,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val requiredQuantity: Long,
    val step01ImpositionId: String? = null,
    val step02GangRunBatchId: String? = null,
    val step03NestingId: String? = null,
    val step04SignatureId: String? = null,
    val step05CtpOutputId: String? = null
)

data class UpdatePrepressPlanStatusRequestDto(
    val status: String,
    val reason: String? = null
)

data class ReconciliationDiscrepancyDto(
    val discrepancyId: String,
    val field: String,
    val sourceStep: String,
    val targetStep: String,
    val expectedValue: String,
    val actualValue: String,
    val severity: String,
    val message: String
)

data class PrepressReconciliationResultDto(
    val isReconciled: Boolean,
    val blockingErrorsCount: Int,
    val warningsCount: Int,
    val discrepancies: List<ReconciliationDiscrepancyDto>,
    val reconciledProducedQuantity: Long,
    val reconciledRequiredSheets: Long,
    val reconciledTotalPages: Int,
    val reconciledSignaturesCount: Int,
    val reconciledPlatesCount: Int,
    val reconciledWastePercentage: BigDecimal,
    val reconciledUtilizationPercentage: BigDecimal,
    val summary: String
)

data class PrepressOptimizationRecommendationDto(
    val recommendationId: String,
    val recommendationType: String,
    val title: String,
    val description: String,
    val affectedStep: String,
    val estimatedWasteReductionPercentage: BigDecimal,
    val estimatedPlateSavingsCount: Int,
    val rationale: String,
    val confidenceScore: BigDecimal,
    val requiresApproval: Boolean,
    val isApplied: Boolean
)

data class PrepressReadinessScoreDto(
    val overallScore: BigDecimal,
    val geometricValidityScore: BigDecimal,
    val nestingEfficiencyScore: BigDecimal,
    val gangRunEfficiencyScore: BigDecimal,
    val sheetUtilizationScore: BigDecimal,
    val signatureValidityScore: BigDecimal,
    val ctpReadinessScore: BigDecimal,
    val integrityVerificationScore: BigDecimal,
    val penaltyPoints: BigDecimal,
    val summary: String
)

data class PipelineStageStatusDto(
    val stageStep: String,
    val stageName: String,
    val isApplicable: Boolean,
    val status: String,
    val referenceId: String?,
    val integrityHash: String?,
    val summary: String
)

data class PrepressOrchestrationPlanDto(
    val planId: String,
    val tenantId: String,
    val planName: String,
    val version: Int,
    val status: String,
    val jobId: String?,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val step01ImpositionId: String?,
    val step01IntegrityHash: String?,
    val step02GangRunBatchId: String?,
    val step02IntegrityHash: String?,
    val step03NestingId: String?,
    val step03IntegrityHash: String?,
    val step04SignatureId: String?,
    val step04IntegrityHash: String?,
    val step05CtpOutputId: String?,
    val step05IntegrityHash: String?,
    val requiredQuantity: Long,
    val totalProducedQuantity: Long,
    val requiredSheets: Long,
    val sheetUtilizationPercentage: BigDecimal,
    val wastePercentage: BigDecimal,
    val totalSignaturesCount: Int,
    val totalPlatesCount: Int,
    val pressSheetWidthMm: BigDecimal,
    val pressSheetHeightMm: BigDecimal,
    val plateWidthMm: BigDecimal,
    val plateHeightMm: BigDecimal,
    val pipelineStages: List<PipelineStageStatusDto>,
    val reconciliationResult: PrepressReconciliationResultDto,
    val readinessScore: PrepressReadinessScoreDto,
    val recommendations: List<PrepressOptimizationRecommendationDto>,
    val masterIntegrityHash: String,
    val approvalStatus: String,
    val approvedBy: String?,
    val approvedAt: Long?,
    val aiHandoffStatus: String,
    val downstreamHandoffStatus: String,
    val notes: String?,
    val createdAt: Long,
    val createdBy: String
)

data class Module18Step06PrepressOrchestrationHandoffContractDto(
    val contractVersion: String,
    val planId: String,
    val tenantId: String,
    val jobId: String?,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val planVersion: Int,
    val planStatus: String,
    val requiredSheets: Long,
    val totalProducedQuantity: Long,
    val totalPlatesCount: Int,
    val totalSignaturesCount: Int,
    val sheetUtilizationPercentage: BigDecimal,
    val wastePercentage: BigDecimal,
    val pressSheetWidthMm: BigDecimal,
    val pressSheetHeightMm: BigDecimal,
    val plateWidthMm: BigDecimal,
    val plateHeightMm: BigDecimal,
    val readinessScore: BigDecimal,
    val isFullyReconciled: Boolean,
    val blockingErrorsCount: Int,
    val warningsCount: Int,
    val masterIntegrityHash: String,
    val step05CtpOutputId: String?,
    val step04SignatureId: String?,
    val step03NestingId: String?,
    val step02GangRunBatchId: String?,
    val step01ImpositionId: String?,
    val generatedAt: Long
)

// Mapping Extension Functions
fun PrepressOrchestrationPlan.toDto(): PrepressOrchestrationPlanDto {
    return PrepressOrchestrationPlanDto(
        planId = planId,
        tenantId = tenantId,
        planName = planName,
        version = version,
        status = status.name,
        jobId = jobId,
        orderId = orderId,
        orderItemId = orderItemId,
        productName = productName,
        step01ImpositionId = step01ImpositionId,
        step01IntegrityHash = step01IntegrityHash,
        step02GangRunBatchId = step02GangRunBatchId,
        step02IntegrityHash = step02GangRunBatchId,
        step03NestingId = step03NestingId,
        step03IntegrityHash = step03IntegrityHash,
        step04SignatureId = step04SignatureId,
        step04IntegrityHash = step04IntegrityHash,
        step05CtpOutputId = step05CtpOutputId,
        step05IntegrityHash = step05IntegrityHash,
        requiredQuantity = requiredQuantity,
        totalProducedQuantity = totalProducedQuantity,
        requiredSheets = requiredSheets,
        sheetUtilizationPercentage = sheetUtilizationPercentage,
        wastePercentage = wastePercentage,
        totalSignaturesCount = totalSignaturesCount,
        totalPlatesCount = totalPlatesCount,
        pressSheetWidthMm = pressSheetWidthMm,
        pressSheetHeightMm = pressSheetHeightMm,
        plateWidthMm = plateWidthMm,
        plateHeightMm = plateHeightMm,
        pipelineStages = pipelineStages.map {
            PipelineStageStatusDto(
                stageStep = it.stageStep,
                stageName = it.stageName,
                isApplicable = it.isApplicable,
                status = it.status,
                referenceId = it.referenceId,
                integrityHash = it.integrityHash,
                summary = it.summary
            )
        },
        reconciliationResult = PrepressReconciliationResultDto(
            isReconciled = reconciliationResult.isReconciled,
            blockingErrorsCount = reconciliationResult.blockingErrorsCount,
            warningsCount = reconciliationResult.warningsCount,
            discrepancies = reconciliationResult.discrepancies.map {
                ReconciliationDiscrepancyDto(
                    discrepancyId = it.discrepancyId,
                    field = it.field,
                    sourceStep = it.sourceStep,
                    targetStep = it.targetStep,
                    expectedValue = it.expectedValue,
                    actualValue = it.actualValue,
                    severity = it.severity.name,
                    message = it.message
                )
            },
            reconciledProducedQuantity = reconciliationResult.reconciledProducedQuantity,
            reconciledRequiredSheets = reconciliationResult.reconciledRequiredSheets,
            reconciledTotalPages = reconciliationResult.reconciledTotalPages,
            reconciledSignaturesCount = reconciliationResult.reconciledSignaturesCount,
            reconciledPlatesCount = reconciliationResult.reconciledPlatesCount,
            reconciledWastePercentage = reconciliationResult.reconciledWastePercentage,
            reconciledUtilizationPercentage = reconciliationResult.reconciledUtilizationPercentage,
            summary = reconciliationResult.summary
        ),
        readinessScore = PrepressReadinessScoreDto(
            overallScore = readinessScore.overallScore,
            geometricValidityScore = readinessScore.geometricValidityScore,
            nestingEfficiencyScore = readinessScore.nestingEfficiencyScore,
            gangRunEfficiencyScore = readinessScore.gangRunEfficiencyScore,
            sheetUtilizationScore = readinessScore.sheetUtilizationScore,
            signatureValidityScore = readinessScore.signatureValidityScore,
            ctpReadinessScore = readinessScore.ctpReadinessScore,
            integrityVerificationScore = readinessScore.integrityVerificationScore,
            penaltyPoints = readinessScore.penaltyPoints,
            summary = readinessScore.summary
        ),
        recommendations = recommendations.map {
            PrepressOptimizationRecommendationDto(
                recommendationId = it.recommendationId,
                recommendationType = it.recommendationType,
                title = it.title,
                description = it.description,
                affectedStep = it.affectedStep,
                estimatedWasteReductionPercentage = it.estimatedWasteReductionPercentage,
                estimatedPlateSavingsCount = it.estimatedPlateSavingsCount,
                rationale = it.rationale,
                confidenceScore = it.confidenceScore,
                requiresApproval = it.requiresApproval,
                isApplied = it.isApplied
            )
        },
        masterIntegrityHash = masterIntegrityHash,
        approvalStatus = approvalStatus,
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        aiHandoffStatus = aiHandoffStatus,
        downstreamHandoffStatus = downstreamHandoffStatus,
        notes = notes,
        createdAt = createdAt,
        createdBy = createdBy
    )
}

fun Module18Step06PrepressOrchestrationHandoffContract.toDto(): Module18Step06PrepressOrchestrationHandoffContractDto {
    return Module18Step06PrepressOrchestrationHandoffContractDto(
        contractVersion = contractVersion,
        planId = planId,
        tenantId = tenantId,
        jobId = jobId,
        orderId = orderId,
        orderItemId = orderItemId,
        productName = productName,
        planVersion = planVersion,
        planStatus = planStatus,
        requiredSheets = requiredSheets,
        totalProducedQuantity = totalProducedQuantity,
        totalPlatesCount = totalPlatesCount,
        totalSignaturesCount = totalSignaturesCount,
        sheetUtilizationPercentage = sheetUtilizationPercentage,
        wastePercentage = wastePercentage,
        pressSheetWidthMm = pressSheetWidthMm,
        pressSheetHeightMm = pressSheetHeightMm,
        plateWidthMm = plateWidthMm,
        plateHeightMm = plateHeightMm,
        readinessScore = readinessScore,
        isFullyReconciled = isFullyReconciled,
        blockingErrorsCount = blockingErrorsCount,
        warningsCount = warningsCount,
        masterIntegrityHash = masterIntegrityHash,
        step05CtpOutputId = step05CtpOutputId,
        step04SignatureId = step04SignatureId,
        step03NestingId = step03NestingId,
        step02GangRunBatchId = step02GangRunBatchId,
        step01ImpositionId = step01ImpositionId,
        generatedAt = generatedAt
    )
}
