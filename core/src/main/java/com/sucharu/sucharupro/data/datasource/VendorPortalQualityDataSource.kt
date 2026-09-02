package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Data Source abstraction for Vendor Portal Quality Workspace.
 */
interface VendorPortalQualityDataSource {
    suspend fun saveQualityCase(case: VendorPortalQualityCase): DomainResult<VendorPortalQualityCase>
    suspend fun findQualityCaseById(tenantId: String, projectId: String, vendorId: String, caseId: String): DomainResult<VendorPortalQualityCase?>
    suspend fun listQualityCases(tenantId: String, projectId: String, vendorId: String, status: VendorPortalQualityCaseStatus?): DomainResult<List<VendorPortalQualityCase>>

    suspend fun saveCapaPlan(capa: VendorPortalCapaPlan): DomainResult<VendorPortalCapaPlan>
    suspend fun findCapaPlanById(tenantId: String, projectId: String, vendorId: String, capaId: String): DomainResult<VendorPortalCapaPlan?>
    suspend fun listCapaPlans(tenantId: String, projectId: String, vendorId: String, status: VendorPortalCapaStatus?, caseId: String?): DomainResult<List<VendorPortalCapaPlan>>

    suspend fun saveCapaAction(action: VendorPortalCapaAction): DomainResult<VendorPortalCapaAction>
    suspend fun listCapaActions(tenantId: String, projectId: String, capaId: String): DomainResult<List<VendorPortalCapaAction>>

    suspend fun saveDisputeSubmission(dispute: VendorPortalDisputeSummary): DomainResult<VendorPortalDisputeSummary>
    suspend fun findDisputeSubmissionById(tenantId: String, projectId: String, vendorId: String, disputeId: String): DomainResult<VendorPortalDisputeSummary?>
    suspend fun listDisputeSubmissions(tenantId: String, projectId: String, vendorId: String, status: VendorPortalDisputeStatus?): DomainResult<List<VendorPortalDisputeSummary>>

    suspend fun saveResolutionResponse(response: VendorPortalResolutionResponse): DomainResult<VendorPortalResolutionResponse>
    suspend fun listResolutionResponses(tenantId: String, projectId: String, vendorId: String, disputeId: String): DomainResult<List<VendorPortalResolutionResponse>>

    suspend fun saveEvidence(evidence: VendorPortalQualityEvidence): DomainResult<VendorPortalQualityEvidence>
    suspend fun findEvidenceById(tenantId: String, projectId: String, vendorId: String, evidenceId: String): DomainResult<VendorPortalQualityEvidence?>
    suspend fun listEvidence(tenantId: String, projectId: String, vendorId: String, entityType: String, entityId: String): DomainResult<List<VendorPortalQualityEvidence>>

    suspend fun recordAudit(activity: VendorPortalQualityActivity): DomainResult<Unit>
    suspend fun listAuditEvents(tenantId: String, projectId: String, vendorId: String, entityType: String, entityId: String): DomainResult<List<VendorPortalQualityActivity>>
}
