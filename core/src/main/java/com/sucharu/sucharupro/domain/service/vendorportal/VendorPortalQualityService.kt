package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.vendor.VendorDisputeType
import com.sucharu.sucharupro.domain.model.vendor.VendorInspectionStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorRejectionStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

/**
 * Input DTOs for Service layer
 */
data class VendorPortalCapaPlanInput(
    val caseId: String? = null,
    val inspectionId: String? = null,
    val rejectionId: String? = null,
    val title: String,
    val rootCause: String,
    val correctiveAction: String,
    val preventiveAction: String,
    val responsiblePerson: String,
    val targetCompletionDate: Long,
    val priority: VendorPortalQualityPriority = VendorPortalQualityPriority.MEDIUM,
    val affectedQuantity: BigDecimal = BigDecimal.ZERO,
    val affectedUnit: String = "PIECE"
)

data class VendorPortalCapaActionInput(
    val actionType: VendorPortalCapaActionType = VendorPortalCapaActionType.CORRECTIVE,
    val description: String,
    val owner: String,
    val targetDate: Long,
    val notes: String? = null
)

data class VendorPortalDisputeInput(
    val sourceType: String, // INSPECTION, REJECTION, INVOICE, DELIVERY_RECEIPT
    val sourceId: String,
    val disputeType: VendorDisputeType = VendorDisputeType.QUALITY,
    val priority: VendorPortalQualityPriority = VendorPortalQualityPriority.MEDIUM,
    val subject: String,
    val description: String,
    val requestedResolution: VendorPortalResolutionType = VendorPortalResolutionType.REPLACEMENT,
    val disputedQuantity: BigDecimal = BigDecimal.ZERO,
    val disputedAmount: Money = Money.ZERO
)

data class VendorPortalQualityEvidenceInput(
    val entityType: String,
    val entityId: String,
    val evidenceType: VendorPortalQualityEvidenceType = VendorPortalQualityEvidenceType.DOCUMENT,
    val filename: String,
    val fileReference: String,
    val sizeBytes: Long = 0L,
    val checksum: String? = null,
    val description: String? = null
)

/**
 * Domain Service Interface for Vendor Quality, CAPA, Rejection & Dispute Workspace.
 */
interface VendorPortalQualityService {
    // Quality Cases
    suspend fun listQualityCases(tenantId: String, projectId: String, vendorId: String, status: VendorPortalQualityCaseStatus? = null): DomainResult<List<VendorPortalQualityCase>>
    suspend fun getQualityCaseById(tenantId: String, projectId: String, vendorId: String, caseId: String): DomainResult<VendorPortalQualityCase>
    suspend fun acknowledgeQualityCase(tenantId: String, projectId: String, vendorId: String, caseId: String, actorId: String): DomainResult<VendorPortalQualityCase>

    // Inspections & Rejections (Read & Projection from Canonical Module 12)
    suspend fun listInspections(tenantId: String, projectId: String, vendorId: String, status: VendorInspectionStatus? = null): DomainResult<List<VendorPortalQualityInspectionSummary>>
    suspend fun getInspectionById(tenantId: String, projectId: String, vendorId: String, inspectionId: String): DomainResult<VendorPortalQualityInspectionSummary>
    suspend fun listRejections(tenantId: String, projectId: String, vendorId: String, status: VendorRejectionStatus? = null): DomainResult<List<VendorPortalRejectionSummary>>
    suspend fun getRejectionById(tenantId: String, projectId: String, vendorId: String, rejectionId: String): DomainResult<VendorPortalRejectionSummary>

    // Responses
    suspend fun respondToQualityCase(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        comment: String,
        correctiveActionPlan: String? = null,
        promisedReplacementDate: Long? = null,
        evidenceReferences: List<String> = emptyList(),
        actorId: String
    ): DomainResult<VendorPortalQualityCase>

    // CAPA
    suspend fun createCapaPlan(tenantId: String, projectId: String, vendorId: String, input: VendorPortalCapaPlanInput, actorId: String): DomainResult<VendorPortalCapaPlan>
    suspend fun getCapaPlanById(tenantId: String, projectId: String, vendorId: String, capaId: String): DomainResult<VendorPortalCapaPlan>
    suspend fun listCapaPlans(tenantId: String, projectId: String, vendorId: String, status: VendorPortalCapaStatus? = null, caseId: String? = null): DomainResult<List<VendorPortalCapaPlan>>
    suspend fun submitCapaPlan(tenantId: String, projectId: String, vendorId: String, capaId: String, actorId: String): DomainResult<VendorPortalCapaPlan>
    suspend fun completeCapaPlan(tenantId: String, projectId: String, vendorId: String, capaId: String, actorId: String): DomainResult<VendorPortalCapaPlan>
    suspend fun addCapaAction(tenantId: String, projectId: String, vendorId: String, capaId: String, actionInput: VendorPortalCapaActionInput, actorId: String): DomainResult<VendorPortalCapaAction>
    suspend fun completeCapaAction(tenantId: String, projectId: String, vendorId: String, capaId: String, actionId: String, evidenceRefs: List<String> = emptyList(), actorId: String): DomainResult<VendorPortalCapaAction>

    // Disputes
    suspend fun createDispute(tenantId: String, projectId: String, vendorId: String, input: VendorPortalDisputeInput, actorId: String): DomainResult<VendorPortalDisputeSummary>
    suspend fun getDisputeById(tenantId: String, projectId: String, vendorId: String, disputeId: String): DomainResult<VendorPortalDisputeSummary>
    suspend fun listDisputes(tenantId: String, projectId: String, vendorId: String, status: VendorPortalDisputeStatus? = null): DomainResult<List<VendorPortalDisputeSummary>>
    suspend fun respondToDispute(tenantId: String, projectId: String, vendorId: String, disputeId: String, response: String, actorId: String): DomainResult<VendorPortalDisputeSummary>
    suspend fun respondToResolutionProposal(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        action: VendorPortalProposalAction,
        rationale: String,
        actorId: String
    ): DomainResult<VendorPortalResolutionResponse>

    // Evidence & Audit
    suspend fun uploadEvidence(tenantId: String, projectId: String, vendorId: String, input: VendorPortalQualityEvidenceInput, actorId: String): DomainResult<VendorPortalQualityEvidence>
    suspend fun listEvidence(tenantId: String, projectId: String, vendorId: String, entityType: String, entityId: String): DomainResult<List<VendorPortalQualityEvidence>>
    suspend fun listQualityActivityTimeline(tenantId: String, projectId: String, vendorId: String, entityType: String, entityId: String): DomainResult<List<VendorPortalQualityActivity>>

    // Workspace & KPIs
    suspend fun getQualityKpiSummary(tenantId: String, projectId: String, vendorId: String): DomainResult<VendorPortalQualityKpiSummary>
    suspend fun getQualityWorkspace(tenantId: String, projectId: String, vendorId: String): DomainResult<VendorPortalQualityWorkspace>
}
