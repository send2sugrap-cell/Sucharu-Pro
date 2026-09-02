package com.sucharu.sucharupro.domain.model.vendor

import java.time.Instant

/**
 * Metric result returned by deterministic calculations.
 */
data class MetricCalculationResult(
    val value: Double,
    val numerator: Double,
    val denominator: Double,
    val unit: String,
    val sampleSize: Int,
    val confidenceState: MeasurementConfidenceState,
    val calculationVersion: String = "1.0"
)

/**
 * KPI Definition entity.
 */
data class VendorPerformanceKpi(
    val kpiId: String,
    val projectId: String,
    val tenantId: String,
    val code: String,
    val name: String,
    val description: String,
    val kpiType: KpiType,
    val measurementMethod: KpiMeasurementMethod = KpiMeasurementMethod.AUTOMATED,
    val targetValue: Double,
    val minimumAcceptableValue: Double? = null,
    val maximumAcceptableValue: Double? = null,
    val unit: String = "%",
    val direction: KpiDirection = KpiDirection.HIGHER_IS_BETTER,
    val weight: Double = 1.0,
    val status: KpiStatus = KpiStatus.ACTIVE,
    val effectiveFrom: Instant = Instant.now(),
    val effectiveTo: Instant? = null,
    val version: Long = 1,
    val createdAt: Instant = Instant.now(),
    val createdBy: String,
    val updatedAt: Instant = Instant.now(),
    val updatedBy: String? = null
)

/**
 * Measurement recorded for a specific KPI, vendor, and evaluation period.
 */
data class VendorPerformanceMeasurement(
    val measurementId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val kpiId: String,
    val kpiCode: String,
    val periodStart: Instant,
    val periodEnd: Instant,
    val actualValue: Double,
    val numerator: Double,
    val denominator: Double,
    val unit: String = "%",
    val sampleSize: Int,
    val confidenceState: MeasurementConfidenceState = MeasurementConfidenceState.SUFFICIENT_DATA,
    val calculationVersion: String = "1.0",
    val measuredAt: Instant = Instant.now(),
    val measuredBy: String
)

/**
 * Line item in a vendor performance scorecard.
 */
data class VendorPerformanceScorecardItem(
    val itemId: String,
    val scorecardId: String,
    val kpiId: String,
    val kpiCode: String,
    val kpiName: String,
    val kpiType: KpiType,
    val weight: Double,
    val direction: KpiDirection,
    val targetValue: Double,
    val actualValue: Double,
    val normalizedScore: Double, // 0.0 .. 100.0
    val weightedScore: Double,
    val numerator: Double,
    val denominator: Double,
    val unit: String = "%",
    val sampleSize: Int,
    val confidenceState: MeasurementConfidenceState = MeasurementConfidenceState.SUFFICIENT_DATA
)

/**
 * Immutable snapshot of a vendor's evaluated performance over a defined period.
 */
data class VendorPerformanceScorecard(
    val scorecardId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val periodType: EvaluationPeriodType = EvaluationPeriodType.MONTHLY,
    val periodStart: Instant,
    val periodEnd: Instant,
    val overallScore: Double, // 0.0 .. 100.0
    val rating: PerformanceRating,
    val riskLevel: ComplianceRiskLevel = ComplianceRiskLevel.LOW,
    val dataCompleteness: Double = 100.0,
    val sampleSize: Int = 0,
    val calculationVersion: String = "1.0",
    val status: ScorecardStatus = ScorecardStatus.DRAFT,
    val items: List<VendorPerformanceScorecardItem> = emptyList(),
    val notes: String? = null,
    val version: Long = 1,
    val generatedAt: Instant = Instant.now(),
    val generatedBy: String,
    val approvedAt: Instant? = null,
    val approvedBy: String? = null
)

/**
 * Criterion evaluated during formal evaluation.
 */
data class VendorEvaluationCriterion(
    val criterionId: String,
    val evaluationId: String,
    val name: String,
    val category: String,
    val weight: Double,
    val score: Double,
    val comments: String? = null
)

/**
 * Formal vendor evaluation workflow aggregate.
 */
data class VendorEvaluation(
    val evaluationId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val scorecardId: String? = null,
    val periodType: EvaluationPeriodType = EvaluationPeriodType.MONTHLY,
    val periodStart: Instant,
    val periodEnd: Instant,
    val evaluatorId: String,
    val evaluatorName: String,
    val status: EvaluationStatus = EvaluationStatus.DRAFT,
    val decision: EvaluationDecision? = null,
    val evaluationScore: Double = 0.0,
    val rating: PerformanceRating = PerformanceRating.ACCEPTABLE,
    val evaluatorComments: String? = null,
    val reviewComments: String? = null,
    val rejectionReason: String? = null,
    val criteria: List<VendorEvaluationCriterion> = emptyList(),
    val submittedAt: Instant? = null,
    val submittedBy: String? = null,
    val reviewedAt: Instant? = null,
    val reviewedBy: String? = null,
    val approvedAt: Instant? = null,
    val approvedBy: String? = null,
    val finalizedAt: Instant? = null,
    val finalizedBy: String? = null,
    val version: Long = 1,
    val createdAt: Instant = Instant.now(),
    val createdBy: String,
    val updatedAt: Instant = Instant.now(),
    val updatedBy: String? = null
)

/**
 * Master requirement definition for vendor compliance.
 */
data class VendorComplianceRequirement(
    val requirementId: String,
    val projectId: String,
    val tenantId: String,
    val requirementType: ComplianceRequirementType,
    val code: String,
    val name: String,
    val description: String,
    val mandatory: Boolean = true,
    val riskLevel: ComplianceRiskLevel = ComplianceRiskLevel.HIGH,
    val validityDays: Int? = 365,
    val status: ComplianceStatus = ComplianceStatus.PENDING,
    val version: Long = 1,
    val createdAt: Instant = Instant.now(),
    val createdBy: String,
    val updatedAt: Instant = Instant.now(),
    val updatedBy: String? = null
)

/**
 * Evidence attached to a vendor compliance record.
 */
data class VendorComplianceEvidence(
    val evidenceId: String,
    val recordId: String,
    val projectId: String,
    val tenantId: String,
    val evidenceType: ComplianceEvidenceType = ComplianceEvidenceType.DOCUMENT,
    val fileName: String,
    val fileUrl: String,
    val checksum: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val uploadedBy: String,
    val uploadedAt: Instant = Instant.now()
)

/**
 * Vendor-specific compliance submission and verification record.
 */
data class VendorComplianceRecord(
    val recordId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val requirementId: String,
    val requirementCode: String,
    val requirementName: String,
    val requirementType: ComplianceRequirementType,
    val mandatory: Boolean = true,
    val effectiveDate: Instant = Instant.now(),
    val expiryDate: Instant? = null,
    val status: ComplianceStatus = ComplianceStatus.PENDING,
    val riskLevel: ComplianceRiskLevel = ComplianceRiskLevel.LOW,
    val verificationStatus: ComplianceVerificationStatus = ComplianceVerificationStatus.PENDING,
    val verifiedBy: String? = null,
    val verifiedAt: Instant? = null,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val evidenceList: List<VendorComplianceEvidence> = emptyList(),
    val version: Long = 1,
    val createdAt: Instant = Instant.now(),
    val createdBy: String,
    val updatedAt: Instant = Instant.now(),
    val updatedBy: String? = null
)

/**
 * Vendor Corrective Action / Improvement Plan (CAPA).
 */
data class VendorCorrectiveAction(
    val actionId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val sourceType: String, // KPI, QUALITY, DISPUTE, EVALUATION, COMPLIANCE
    val sourceId: String? = null,
    val issueDescription: String,
    val rootCause: String? = null,
    val actionPlan: String,
    val assignedTo: String,
    val assignedToName: String,
    val priority: CorrectiveActionPriority = CorrectiveActionPriority.MEDIUM,
    val dueDate: Instant,
    val status: CorrectiveActionStatus = CorrectiveActionStatus.OPEN,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val closedAt: Instant? = null,
    val verificationNotes: String? = null,
    val verifiedBy: String? = null,
    val verifiedAt: Instant? = null,
    val version: Long = 1,
    val createdAt: Instant = Instant.now(),
    val createdBy: String,
    val updatedAt: Instant = Instant.now(),
    val updatedBy: String? = null
)

/**
 * Explainable vendor risk indicator.
 */
data class VendorRiskIndicator(
    val riskId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val riskType: RiskIndicatorType,
    val severity: RiskSeverity,
    val source: String,
    val sourceId: String? = null,
    val title: String,
    val description: String,
    val evidenceReference: String? = null,
    val detectedAt: Instant = Instant.now(),
    val status: RiskStatus = RiskStatus.ACTIVE
)

/**
 * Append-only immutable audit trail record.
 */
data class VendorPerformanceAuditEvent(
    val auditId: String,
    val projectId: String,
    val tenantId: String,
    val entityType: String,
    val entityId: String,
    val eventType: VendorPerformanceAuditEventType,
    val action: String,
    val actorId: String,
    val actorRole: String? = null,
    val details: String? = null,
    val occurredAt: Instant = Instant.now()
)

/**
 * Vendor performance trend data point across evaluation periods.
 */
data class VendorPerformanceTrendPoint(
    val periodStart: Instant,
    val periodEnd: Instant,
    val overallScore: Double,
    val qualityScore: Double,
    val deliveryScore: Double,
    val costScore: Double,
    val complianceScore: Double,
    val disputeCount: Int,
    val rating: PerformanceRating
)
