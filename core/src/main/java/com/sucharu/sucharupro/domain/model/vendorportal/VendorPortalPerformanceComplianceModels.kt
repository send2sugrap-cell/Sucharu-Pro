package com.sucharu.sucharupro.domain.model.vendorportal

import com.sucharu.sucharupro.domain.model.vendor.*
import java.time.Instant

/**
 * Vendor-facing summary of an individual Performance KPI.
 */
data class VendorPortalPerformanceKpiSummary(
    val kpiId: String,
    val code: String,
    val name: String,
    val description: String,
    val kpiType: KpiType,
    val targetValue: Double,
    val actualValue: Double,
    val normalizedScore: Double, // 0.0 .. 100.0
    val weightedScore: Double,
    val weight: Double,
    val unit: String = "%",
    val direction: KpiDirection = KpiDirection.HIGHER_IS_BETTER,
    val sampleSize: Int = 0,
    val confidenceState: MeasurementConfidenceState = MeasurementConfidenceState.SUFFICIENT_DATA
)

/**
 * Vendor-facing scorecard summary projection from Module 12.
 */
data class VendorPortalPerformanceScorecardSummary(
    val scorecardId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val periodType: EvaluationPeriodType,
    val periodStart: Long,
    val periodEnd: Long,
    val overallScore: Double,
    val rating: PerformanceRating,
    val riskLevel: ComplianceRiskLevel,
    val dataCompleteness: Double,
    val sampleSize: Int,
    val status: ScorecardStatus,
    val notes: String? = null,
    val items: List<VendorPortalPerformanceKpiSummary> = emptyList(),
    val generatedAt: Long,
    val approvedAt: Long? = null
)

/**
 * Historical trend data point for vendor performance charts.
 */
data class VendorPortalPerformanceTrendPoint(
    val periodStart: Long,
    val periodEnd: Long,
    val overallScore: Double,
    val qualityScore: Double,
    val deliveryScore: Double,
    val costScore: Double,
    val complianceScore: Double,
    val disputeCount: Int,
    val rating: PerformanceRating
)

/**
 * Performance Overview containing high-level score, trend, rating and KPI highlights.
 */
data class VendorPortalPerformanceOverview(
    val vendorId: String,
    val overallScore: Double,
    val rating: PerformanceRating,
    val riskLevel: ComplianceRiskLevel,
    val onTimeDeliveryRate: Double,
    val poFulfillmentRate: Double,
    val defectRate: Double,
    val qualityRating: String,
    val totalScorecards: Int,
    val activeEvaluations: Int,
    val openCorrectiveActions: Int,
    val latestPeriodStart: Long? = null,
    val latestPeriodEnd: Long? = null,
    val topStrengths: List<String> = emptyList(),
    val improvementAreas: List<String> = emptyList()
)

/**
 * Evaluation feedback criterion visible to vendor.
 */
data class VendorPortalEvaluationCriterionSummary(
    val criterionId: String,
    val name: String,
    val category: String,
    val weight: Double,
    val score: Double,
    val comments: String? = null
)

/**
 * Vendor-facing projection of a formal Evaluation aggregate.
 */
data class VendorPortalEvaluationSummary(
    val evaluationId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val scorecardId: String? = null,
    val periodType: EvaluationPeriodType,
    val periodStart: Long,
    val periodEnd: Long,
    val status: EvaluationStatus,
    val decision: EvaluationDecision? = null,
    val evaluationScore: Double,
    val rating: PerformanceRating,
    val evaluatorComments: String? = null,
    val reviewComments: String? = null,
    val criteria: List<VendorPortalEvaluationCriterionSummary> = emptyList(),
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val finalizedAt: Long? = null,
    val createdAt: Long
)

/**
 * Formal Vendor Response to an Evaluation.
 */
data class VendorPortalEvaluationResponse(
    val responseId: String,
    val evaluationId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val responseType: VendorPortalEvaluationResponseType = VendorPortalEvaluationResponseType.FORMAL_RESPONSE,
    val subject: String,
    val remarks: String,
    val proposedRemediation: String? = null,
    val evidenceReferences: List<String> = emptyList(),
    val status: VendorPortalEvaluationResponseStatus = VendorPortalEvaluationResponseStatus.SUBMITTED,
    val submittedBy: String,
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewerFeedback: String? = null,
    val version: Long = 1
)

/**
 * Vendor-facing Compliance Requirement Summary.
 */
data class VendorPortalComplianceRequirementSummary(
    val requirementId: String,
    val requirementType: ComplianceRequirementType,
    val code: String,
    val name: String,
    val description: String,
    val mandatory: Boolean,
    val riskLevel: ComplianceRiskLevel,
    val validityDays: Int?
)

/**
 * Vendor-facing Compliance Evidence metadata.
 */
data class VendorPortalComplianceEvidence(
    val evidenceId: String,
    val recordId: String? = null,
    val requirementId: String? = null,
    val actionId: String? = null,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val evidenceType: VendorPortalComplianceEvidenceType = VendorPortalComplianceEvidenceType.DOCUMENT,
    val fileName: String,
    val fileUrl: String,
    val checksum: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val description: String? = null,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis(),
    val version: Long = 1
)

/**
 * Vendor-facing Compliance Record & Certification Summary.
 */
data class VendorPortalComplianceRecordSummary(
    val recordId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val requirementId: String,
    val requirementCode: String,
    val requirementName: String,
    val requirementType: ComplianceRequirementType,
    val mandatory: Boolean,
    val effectiveDate: Long,
    val expiryDate: Long?,
    val status: ComplianceStatus,
    val riskLevel: ComplianceRiskLevel,
    val verificationStatus: ComplianceVerificationStatus,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val daysUntilExpiry: Long? = null,
    val expiryAlertLevel: VendorPortalExpiryAlertLevel = VendorPortalExpiryAlertLevel.NORMAL,
    val evidenceCount: Int = 0,
    val evidenceList: List<VendorPortalComplianceEvidence> = emptyList()
)

/**
 * Certification Expiry Alert.
 */
data class VendorPortalCertificationExpiryAlert(
    val recordId: String,
    val certificationName: String,
    val requirementCode: String,
    val expiryDate: Long,
    val daysRemaining: Long,
    val alertLevel: VendorPortalExpiryAlertLevel,
    val mandatory: Boolean,
    val status: ComplianceStatus
)

/**
 * Vendor-facing Compliance Risk Overview.
 */
data class VendorPortalComplianceOverview(
    val vendorId: String,
    val overallRiskLevel: ComplianceRiskLevel,
    val overallComplianceStatus: ComplianceStatus,
    val totalRequirements: Int,
    val compliantCount: Int,
    val pendingCount: Int,
    val nonCompliantCount: Int,
    val expiredCertificationsCount: Int,
    val upcomingExpiringCertificationsCount: Int,
    val openCorrectiveActionsCount: Int,
    val complianceRate: Double
)

/**
 * Vendor-facing Corrective Action (CAPA) Summary.
 */
data class VendorPortalCorrectiveActionSummary(
    val actionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val sourceType: String, // KPI, QUALITY, DISPUTE, EVALUATION, COMPLIANCE
    val sourceId: String? = null,
    val issueDescription: String,
    val rootCause: String? = null,
    val actionPlan: String,
    val priority: CorrectiveActionPriority,
    val dueDate: Long,
    val status: CorrectiveActionStatus,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val closedAt: Long? = null,
    val isOverdue: Boolean = false,
    val latestVendorResponse: String? = null,
    val responsesCount: Int = 0
)

/**
 * Vendor Remediation / Progress Response to an assigned Corrective Action.
 */
data class VendorPortalCorrectiveActionResponse(
    val responseId: String,
    val actionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val remediationNotes: String,
    val rootCauseExplanation: String? = null,
    val progressPercentage: Double = 0.0,
    val isCompletionRequest: Boolean = false,
    val evidenceReferences: List<String> = emptyList(),
    val status: VendorPortalRemediationStatus = VendorPortalRemediationStatus.PLAN_SUBMITTED,
    val submittedBy: String,
    val submittedAt: Long = System.currentTimeMillis(),
    val version: Long = 1
)

/**
 * Immutable Activity / Audit log entry for Performance & Compliance portal interactions.
 */
data class VendorPortalPerformanceComplianceActivity(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val eventType: VendorPortalPerformanceComplianceAuditEventType,
    val entityType: String, // EVALUATION, SCORECARD, COMPLIANCE_RECORD, CORRECTIVE_ACTION, EVIDENCE
    val entityId: String,
    val actorId: String,
    val actorRole: String? = null,
    val description: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Complete consolidated Workspace payload.
 */
data class VendorPortalPerformanceWorkspace(
    val overview: VendorPortalPerformanceOverview,
    val complianceOverview: VendorPortalComplianceOverview,
    val recentScorecards: List<VendorPortalPerformanceScorecardSummary> = emptyList(),
    val pendingEvaluations: List<VendorPortalEvaluationSummary> = emptyList(),
    val urgentExpiries: List<VendorPortalCertificationExpiryAlert> = emptyList(),
    val openCorrectiveActions: List<VendorPortalCorrectiveActionSummary> = emptyList()
)
