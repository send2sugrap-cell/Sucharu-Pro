package com.sucharu.sucharupro.domain.model.imposition

import java.math.BigDecimal

/**
 * Master Prepress Orchestration Plan Lifecycle Status.
 * Module 18 Step 06.
 */
enum class PrepressPlanStatus {
    DRAFT,
    VALIDATING,
    VALIDATED,
    WARNING,
    READY,
    APPROVED,
    FINALIZED,
    SUPERSEDED,
    REJECTED
}

/**
 * Discrepancy Severity Level for Cross-Step Imposition Reconciliation.
 * Module 18 Step 06.
 */
enum class ReconciliationSeverity {
    INFO,
    WARNING,
    BLOCKING_ERROR
}

/**
 * Individual Cross-Step Reconciliation Diagnostic Item.
 * Module 18 Step 06.
 */
data class ReconciliationDiscrepancy(
    val discrepancyId: String = java.util.UUID.randomUUID().toString(),
    val field: String,
    val sourceStep: String,
    val targetStep: String,
    val expectedValue: String,
    val actualValue: String,
    val severity: ReconciliationSeverity,
    val message: String
)

/**
 * Overall Cross-Step Reconciliation Result.
 * Module 18 Step 06.
 */
data class PrepressReconciliationResult(
    val isReconciled: Boolean,
    val blockingErrorsCount: Int,
    val warningsCount: Int,
    val discrepancies: List<ReconciliationDiscrepancy>,
    val reconciledProducedQuantity: Long,
    val reconciledRequiredSheets: Long,
    val reconciledTotalPages: Int,
    val reconciledSignaturesCount: Int,
    val reconciledPlatesCount: Int,
    val reconciledWastePercentage: BigDecimal,
    val reconciledUtilizationPercentage: BigDecimal,
    val summary: String
)

/**
 * Deterministic Optimization Recommendation emitted by the Prepress Intelligence Engine.
 * Module 18 Step 06.
 */
data class PrepressOptimizationRecommendation(
    val recommendationId: String = java.util.UUID.randomUUID().toString(),
    val recommendationType: String,
    val title: String,
    val description: String,
    val affectedStep: String,
    val estimatedWasteReductionPercentage: BigDecimal,
    val estimatedPlateSavingsCount: Int = 0,
    val rationale: String,
    val confidenceScore: BigDecimal,
    val requiresApproval: Boolean = true,
    val isApplied: Boolean = false
)

/**
 * Multi-Dimensional Readiness / Quality Score (0–100 scale, scale = 4, HALF_UP).
 * Module 18 Step 06.
 */
data class PrepressReadinessScore(
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

/**
 * Upstream Pipeline Stage Reference and Verification Status.
 * Module 18 Step 06.
 */
data class PipelineStageStatus(
    val stageStep: String,
    val stageName: String,
    val isApplicable: Boolean,
    val status: String,
    val referenceId: String?,
    val integrityHash: String?,
    val summary: String
)

/**
 * Authoritative Master Prepress Orchestration Plan Aggregate Root.
 * Module 18 Step 06.
 */
data class PrepressOrchestrationPlan(
    val planId: String,
    val tenantId: String,
    val planName: String,
    val version: Int = 1,
    val status: PrepressPlanStatus = PrepressPlanStatus.DRAFT,

    // Source Business Identifiers
    val jobId: String?,
    val orderId: String,
    val orderItemId: String,
    val productName: String,

    // Upstream Step References & Hashes
    val step01ImpositionId: String? = null,
    val step01IntegrityHash: String? = null,
    val step02GangRunBatchId: String? = null,
    val step02IntegrityHash: String? = null,
    val step03NestingId: String? = null,
    val step03IntegrityHash: String? = null,
    val step04SignatureId: String? = null,
    val step04IntegrityHash: String? = null,
    val step05CtpOutputId: String? = null,
    val step05IntegrityHash: String? = null,

    // Reconciled Metrics
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

    // Pipeline Stage Diagnostics
    val pipelineStages: List<PipelineStageStatus> = emptyList(),

    // Cross-Step Reconciliation Result
    val reconciliationResult: PrepressReconciliationResult,

    // Readiness & Quality Score
    val readinessScore: PrepressReadinessScore,

    // Optimization Recommendations
    val recommendations: List<PrepressOptimizationRecommendation> = emptyList(),

    // Cryptographic Master Integrity Seal (SHA-256)
    val masterIntegrityHash: String,

    // Governance & Audit
    val approvalStatus: String = "PENDING_REVIEW",
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val aiHandoffStatus: String = "READY_FOR_HANDOFF",
    val downstreamHandoffStatus: String = "EMITTED",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String
)

/**
 * Sealed Downstream Prepress Orchestration Handoff Contract for Module 19 / 17 / AI Agents.
 * Module 18 Step 06.
 */
data class Module18Step06PrepressOrchestrationHandoffContract(
    val contractVersion: String = "1.0.0",
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
    val generatedAt: Long = System.currentTimeMillis()
)
