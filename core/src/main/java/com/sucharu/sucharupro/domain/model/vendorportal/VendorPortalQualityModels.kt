package com.sucharu.sucharupro.domain.model.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import java.math.BigDecimal

/**
 * Quality Case Lifecycle Status for Vendor Portal Workspace (Module 13 Step 07).
 */
enum class VendorPortalQualityCaseStatus {
    OPEN,
    ACKNOWLEDGED,
    RESPONSE_REQUIRED,
    RESPONSE_SUBMITTED,
    CAPA_REQUIRED,
    CAPA_SUBMITTED,
    UNDER_REVIEW,
    RESOLVED,
    CLOSED
}

/**
 * CAPA Plan Status for Vendor Portal.
 */
enum class VendorPortalCapaStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    IN_PROGRESS,
    COMPLETED,
    OVERDUE,
    CLOSED
}

/**
 * CAPA Action Item Status.
 */
enum class VendorPortalCapaActionStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    VERIFIED,
    FAILED,
    CANCELLED
}

/**
 * CAPA Action Classification.
 */
enum class VendorPortalCapaActionType {
    CORRECTIVE,
    PREVENTIVE,
    CONTAINMENT
}

/**
 * Vendor Portal Dispute Status.
 */
enum class VendorPortalDisputeStatus {
    OPEN,
    VENDOR_RESPONSE_REQUIRED,
    VENDOR_RESPONDED,
    UNDER_REVIEW,
    RESOLUTION_PROPOSED,
    RESOLUTION_ACCEPTED,
    RESOLUTION_REJECTED,
    ESCALATED,
    RESOLVED,
    CLOSED
}

/**
 * Proposed / Agreed Resolution Type.
 */
enum class VendorPortalResolutionType {
    REPLACEMENT,
    REWORK,
    CREDIT,
    PRICE_ADJUSTMENT,
    PARTIAL_ACCEPTANCE,
    FULL_REJECTION,
    OTHER
}

/**
 * Vendor Action on Resolution Proposals.
 */
enum class VendorPortalProposalAction {
    ACCEPT,
    REJECT,
    REQUEST_CLARIFICATION
}

/**
 * Quality Evidence Classification.
 */
enum class VendorPortalQualityEvidenceType {
    PHOTO,
    DOCUMENT,
    REPORT,
    CERTIFICATE,
    INSPECTION_RESULT,
    CAPA_DOCUMENT,
    OTHER
}

/**
 * Priority for Quality Cases, CAPA, and Disputes.
 */
enum class VendorPortalQualityPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

/**
 * Vendor Portal Quality Case model.
 */
data class VendorPortalQualityCase(
    val caseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val inspectionId: String? = null,
    val deliveryReceiptId: String? = null,
    val purchaseOrderId: String? = null,
    val rejectionId: String? = null,
    val caseNumber: String,
    val status: VendorPortalQualityCaseStatus = VendorPortalQualityCaseStatus.OPEN,
    val title: String,
    val description: String,
    val severity: VendorDefectSeverity = VendorDefectSeverity.MEDIUM,
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val closedAt: Long? = null,
    val closedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Rejection record projected to the Vendor Portal.
 */
data class VendorPortalRejectionSummary(
    val rejectionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val rejectionReference: String,
    val purchaseOrderId: String? = null,
    val orderNumber: String? = null,
    val deliveryReceiptId: String? = null,
    val receiptNumber: String? = null,
    val inspectionId: String? = null,
    val rejectionType: String,
    val rejectionReason: String,
    val rejectedQuantity: BigDecimal,
    val rejectedValue: Money,
    val status: VendorRejectionStatus,
    val disposition: VendorRejectionDisposition,
    val replacementRequired: Boolean,
    val returnRequired: Boolean,
    val creditRequired: Boolean,
    val vendorResponse: String? = null,
    val vendorResponseAt: Long? = null,
    val resolutionNotes: String? = null,
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * CAPA Plan Domain Model.
 */
data class VendorPortalCapaPlan(
    val capaId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val caseId: String? = null,
    val inspectionId: String? = null,
    val rejectionId: String? = null,
    val capaNumber: String,
    val status: VendorPortalCapaStatus = VendorPortalCapaStatus.DRAFT,
    val priority: VendorPortalQualityPriority = VendorPortalQualityPriority.MEDIUM,
    val title: String,
    val rootCause: String,
    val correctiveAction: String,
    val preventiveAction: String,
    val responsiblePerson: String,
    val targetCompletionDate: Long,
    val actualCompletionDate: Long? = null,
    val affectedQuantity: BigDecimal = BigDecimal.ZERO,
    val affectedUnit: String = "PIECE",
    val verificationStatus: String = "UNVERIFIED",
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val reviewerComments: String? = null,
    val actions: List<VendorPortalCapaAction> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Individual action under a CAPA Plan.
 */
data class VendorPortalCapaAction(
    val actionId: String,
    val capaId: String,
    val tenantId: String,
    val projectId: String,
    val actionNumber: Int = 1,
    val actionType: VendorPortalCapaActionType = VendorPortalCapaActionType.CORRECTIVE,
    val description: String,
    val owner: String,
    val targetDate: Long,
    val status: VendorPortalCapaActionStatus = VendorPortalCapaActionStatus.OPEN,
    val completedAt: Long? = null,
    val evidenceReferences: List<String> = emptyList(),
    val notes: String? = null
)

/**
 * Dispute Record projected to Vendor Portal.
 */
data class VendorPortalDisputeSummary(
    val disputeId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val disputeReference: String,
    val sourceType: String,
    val sourceId: String,
    val disputeType: VendorDisputeType,
    val priority: VendorPortalQualityPriority,
    val status: VendorPortalDisputeStatus,
    val subject: String,
    val description: String,
    val requestedResolution: VendorPortalResolutionType,
    val disputedQuantity: BigDecimal,
    val disputedAmount: Money,
    val raisedBy: String,
    val vendorResponse: String? = null,
    val vendorResponseAt: Long? = null,
    val resolutionProposal: String? = null,
    val resolution: String? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Resolution Response submitted by a Vendor.
 */
data class VendorPortalResolutionResponse(
    val responseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val disputeId: String,
    val proposalAction: VendorPortalProposalAction,
    val rationale: String,
    val respondedBy: String,
    val respondedAt: Long = System.currentTimeMillis()
)

/**
 * Quality Evidence Record.
 */
data class VendorPortalQualityEvidence(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String, // QUALITY_CASE, INSPECTION, REJECTION, CAPA, DISPUTE
    val entityId: String,
    val evidenceType: VendorPortalQualityEvidenceType = VendorPortalQualityEvidenceType.DOCUMENT,
    val filename: String,
    val fileReference: String,
    val sizeBytes: Long = 0L,
    val checksum: String? = null,
    val description: String? = null,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

/**
 * Quality Activity Timeline Item.
 */
data class VendorPortalQualityActivity(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val actorId: String,
    val details: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Quality KPI aggregates for Vendor Portal.
 */
data class VendorPortalQualityKpiSummary(
    val vendorId: String,
    val openQualityCases: Int = 0,
    val pendingVendorResponses: Int = 0,
    val activeCapaCount: Int = 0,
    val overdueCapaCount: Int = 0,
    val openDisputesCount: Int = 0,
    val totalInspectionsCount: Int = 0,
    val totalRejectionsCount: Int = 0,
    val totalRejectedQuantity: BigDecimal = BigDecimal.ZERO,
    val totalAcceptedQuantity: BigDecimal = BigDecimal.ZERO,
    val qualityPassRate: BigDecimal = BigDecimal("100.00")
)

/**
 * Complete Quality Workspace container.
 */
data class VendorPortalQualityWorkspace(
    val kpiSummary: VendorPortalQualityKpiSummary,
    val recentCases: List<VendorPortalQualityCase> = emptyList(),
    val recentInspections: List<VendorPortalQualityInspectionSummary> = emptyList(),
    val recentRejections: List<VendorPortalRejectionSummary> = emptyList(),
    val activeCapas: List<VendorPortalCapaPlan> = emptyList(),
    val activeDisputes: List<VendorPortalDisputeSummary> = emptyList()
)
