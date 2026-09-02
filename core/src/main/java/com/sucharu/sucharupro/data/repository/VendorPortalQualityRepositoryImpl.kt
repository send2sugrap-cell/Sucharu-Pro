package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPortalQualityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalQualityRepository

/**
 * Implementation of VendorPortalQualityRepository delegating to VendorPortalQualityDataSource.
 */
class VendorPortalQualityRepositoryImpl(
    private val dataSource: VendorPortalQualityDataSource
) : VendorPortalQualityRepository {

    override suspend fun saveQualityCase(case: VendorPortalQualityCase): DomainResult<VendorPortalQualityCase> =
        dataSource.saveQualityCase(case)

    override suspend fun findQualityCaseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String
    ): DomainResult<VendorPortalQualityCase?> =
        dataSource.findQualityCaseById(tenantId, projectId, vendorId, caseId)

    override suspend fun listQualityCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalQualityCaseStatus?
    ): DomainResult<List<VendorPortalQualityCase>> =
        dataSource.listQualityCases(tenantId, projectId, vendorId, status)

    override suspend fun saveCapaPlan(capa: VendorPortalCapaPlan): DomainResult<VendorPortalCapaPlan> =
        dataSource.saveCapaPlan(capa)

    override suspend fun findCapaPlanById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        capaId: String
    ): DomainResult<VendorPortalCapaPlan?> =
        dataSource.findCapaPlanById(tenantId, projectId, vendorId, capaId)

    override suspend fun listCapaPlans(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalCapaStatus?,
        caseId: String?
    ): DomainResult<List<VendorPortalCapaPlan>> =
        dataSource.listCapaPlans(tenantId, projectId, vendorId, status, caseId)

    override suspend fun saveCapaAction(action: VendorPortalCapaAction): DomainResult<VendorPortalCapaAction> =
        dataSource.saveCapaAction(action)

    override suspend fun listCapaActions(
        tenantId: String,
        projectId: String,
        capaId: String
    ): DomainResult<List<VendorPortalCapaAction>> =
        dataSource.listCapaActions(tenantId, projectId, capaId)

    override suspend fun saveDisputeSubmission(dispute: VendorPortalDisputeSummary): DomainResult<VendorPortalDisputeSummary> =
        dataSource.saveDisputeSubmission(dispute)

    override suspend fun findDisputeSubmissionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<VendorPortalDisputeSummary?> =
        dataSource.findDisputeSubmissionById(tenantId, projectId, vendorId, disputeId)

    override suspend fun listDisputeSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDisputeStatus?
    ): DomainResult<List<VendorPortalDisputeSummary>> =
        dataSource.listDisputeSubmissions(tenantId, projectId, vendorId, status)

    override suspend fun saveResolutionResponse(response: VendorPortalResolutionResponse): DomainResult<VendorPortalResolutionResponse> =
        dataSource.saveResolutionResponse(response)

    override suspend fun listResolutionResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<List<VendorPortalResolutionResponse>> =
        dataSource.listResolutionResponses(tenantId, projectId, vendorId, disputeId)

    override suspend fun saveEvidence(evidence: VendorPortalQualityEvidence): DomainResult<VendorPortalQualityEvidence> =
        dataSource.saveEvidence(evidence)

    override suspend fun findEvidenceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evidenceId: String
    ): DomainResult<VendorPortalQualityEvidence?> =
        dataSource.findEvidenceById(tenantId, projectId, vendorId, evidenceId)

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalQualityEvidence>> =
        dataSource.listEvidence(tenantId, projectId, vendorId, entityType, entityId)

    override suspend fun recordAudit(activity: VendorPortalQualityActivity): DomainResult<Unit> =
        dataSource.recordAudit(activity)

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalQualityActivity>> =
        dataSource.listAuditEvents(tenantId, projectId, vendorId, entityType, entityId)
}
