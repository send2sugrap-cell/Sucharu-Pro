package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Repository interface for Vendor Portal Quality, CAPA, Rejection & Dispute Workspace (Module 13 Step 07).
 */
interface VendorPortalQualityRepository {
    // Quality Cases
    suspend fun saveQualityCase(case: VendorPortalQualityCase): DomainResult<VendorPortalQualityCase>
    suspend fun findQualityCaseById(tenantId: String, projectId: String, vendorId: String, caseId: String): DomainResult<VendorPortalQualityCase?>
    suspend fun listQualityCases(tenantId: String, projectId: String, vendorId: String, status: VendorPortalQualityCaseStatus? = null): DomainResult<List<VendorPortalQualityCase>>

    // CAPA Plans & Actions
    suspend fun saveCapaPlan(capa: VendorPortalCapaPlan): DomainResult<VendorPortalCapaPlan>
    suspend fun findCapaPlanById(tenantId: String, projectId: String, vendorId: String, capaId: String): DomainResult<VendorPortalCapaPlan?>
    suspend fun listCapaPlans(tenantId: String, projectId: String, vendorId: String, status: VendorPortalCapaStatus? = null, caseId: String? = null): DomainResult<List<VendorPortalCapaPlan>>
    suspend fun saveCapaAction(action: VendorPortalCapaAction): DomainResult<VendorPortalCapaAction>
    suspend fun listCapaActions(tenantId: String, projectId: String, capaId: String): DomainResult<List<VendorPortalCapaAction>>

    // Dispute Submissions
    suspend fun saveDisputeSubmission(dispute: VendorPortalDisputeSummary): DomainResult<VendorPortalDisputeSummary>
    suspend fun findDisputeSubmissionById(tenantId: String, projectId: String, vendorId: String, disputeId: String): DomainResult<VendorPortalDisputeSummary?>
    suspend fun listDisputeSubmissions(tenantId: String, projectId: String, vendorId: String, status: VendorPortalDisputeStatus? = null): DomainResult<List<VendorPortalDisputeSummary>>

    // Resolution Responses
    suspend fun saveResolutionResponse(response: VendorPortalResolutionResponse): DomainResult<VendorPortalResolutionResponse>
    suspend fun listResolutionResponses(tenantId: String, projectId: String, vendorId: String, disputeId: String): DomainResult<List<VendorPortalResolutionResponse>>

    // Evidence
    suspend fun saveEvidence(evidence: VendorPortalQualityEvidence): DomainResult<VendorPortalQualityEvidence>
    suspend fun findEvidenceById(tenantId: String, projectId: String, vendorId: String, evidenceId: String): DomainResult<VendorPortalQualityEvidence?>
    suspend fun listEvidence(tenantId: String, projectId: String, vendorId: String, entityType: String, entityId: String): DomainResult<List<VendorPortalQualityEvidence>>

    // Audit Events
    suspend fun recordAudit(activity: VendorPortalQualityActivity): DomainResult<Unit>
    suspend fun listAuditEvents(tenantId: String, projectId: String, vendorId: String, entityType: String, entityId: String): DomainResult<List<VendorPortalQualityActivity>>
}
