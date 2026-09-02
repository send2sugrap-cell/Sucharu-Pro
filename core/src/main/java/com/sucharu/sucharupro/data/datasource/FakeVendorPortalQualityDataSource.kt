package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Data Source for Vendor Portal Quality Workspace.
 */
class FakeVendorPortalQualityDataSource : VendorPortalQualityDataSource {

    private val lock = Any()
    private val cases = ConcurrentHashMap<String, VendorPortalQualityCase>()
    private val capaPlans = ConcurrentHashMap<String, VendorPortalCapaPlan>()
    private val capaActions = ConcurrentHashMap<String, MutableList<VendorPortalCapaAction>>()
    private val disputes = ConcurrentHashMap<String, VendorPortalDisputeSummary>()
    private val resolutionResponses = ConcurrentHashMap<String, MutableList<VendorPortalResolutionResponse>>()
    private val evidenceMap = ConcurrentHashMap<String, VendorPortalQualityEvidence>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<VendorPortalQualityActivity>>()

    override suspend fun saveQualityCase(case: VendorPortalQualityCase): DomainResult<VendorPortalQualityCase> {
        synchronized(lock) {
            val key = "${case.tenantId}:${case.projectId}:${case.vendorId}:${case.caseId}"
            cases[key] = case
            return DomainResult.Success(case)
        }
    }

    override suspend fun findQualityCaseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String
    ): DomainResult<VendorPortalQualityCase?> {
        synchronized(lock) {
            val key = "$tenantId:$projectId:$vendorId:$caseId"
            return DomainResult.Success(cases[key])
        }
    }

    override suspend fun listQualityCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalQualityCaseStatus?
    ): DomainResult<List<VendorPortalQualityCase>> {
        synchronized(lock) {
            val res = cases.values
                .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
                .filter { status == null || it.status == status }
                .sortedByDescending { it.createdAt }
            return DomainResult.Success(res)
        }
    }

    override suspend fun saveCapaPlan(capa: VendorPortalCapaPlan): DomainResult<VendorPortalCapaPlan> {
        synchronized(lock) {
            val key = "${capa.tenantId}:${capa.projectId}:${capa.vendorId}:${capa.capaId}"
            capaPlans[key] = capa
            return DomainResult.Success(capa)
        }
    }

    override suspend fun findCapaPlanById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        capaId: String
    ): DomainResult<VendorPortalCapaPlan?> {
        synchronized(lock) {
            val key = "$tenantId:$projectId:$vendorId:$capaId"
            val plan = capaPlans[key] ?: return DomainResult.Success(null)
            val actions = capaActions[capaId] ?: emptyList()
            return DomainResult.Success(plan.copy(actions = actions))
        }
    }

    override suspend fun listCapaPlans(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalCapaStatus?,
        caseId: String?
    ): DomainResult<List<VendorPortalCapaPlan>> {
        synchronized(lock) {
            val res = capaPlans.values
                .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
                .filter { status == null || it.status == status }
                .filter { caseId == null || it.caseId == caseId }
                .map { plan -> plan.copy(actions = capaActions[plan.capaId] ?: emptyList()) }
                .sortedByDescending { it.createdAt }
            return DomainResult.Success(res)
        }
    }

    override suspend fun saveCapaAction(action: VendorPortalCapaAction): DomainResult<VendorPortalCapaAction> {
        synchronized(lock) {
            val list = capaActions.getOrPut(action.capaId) { mutableListOf() }
            list.removeIf { it.actionId == action.actionId }
            list.add(action)
            return DomainResult.Success(action)
        }
    }

    override suspend fun listCapaActions(
        tenantId: String,
        projectId: String,
        capaId: String
    ): DomainResult<List<VendorPortalCapaAction>> {
        synchronized(lock) {
            val list = (capaActions[capaId] ?: emptyList())
                .filter { it.tenantId == tenantId && it.projectId == projectId }
                .sortedBy { it.actionNumber }
            return DomainResult.Success(list)
        }
    }

    override suspend fun saveDisputeSubmission(dispute: VendorPortalDisputeSummary): DomainResult<VendorPortalDisputeSummary> {
        synchronized(lock) {
            val key = "${dispute.tenantId}:${dispute.projectId}:${dispute.vendorId}:${dispute.disputeId}"
            disputes[key] = dispute
            return DomainResult.Success(dispute)
        }
    }

    override suspend fun findDisputeSubmissionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<VendorPortalDisputeSummary?> {
        synchronized(lock) {
            val key = "$tenantId:$projectId:$vendorId:$disputeId"
            return DomainResult.Success(disputes[key])
        }
    }

    override suspend fun listDisputeSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDisputeStatus?
    ): DomainResult<List<VendorPortalDisputeSummary>> {
        synchronized(lock) {
            val res = disputes.values
                .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
                .filter { status == null || it.status == status }
                .sortedByDescending { it.createdAt }
            return DomainResult.Success(res)
        }
    }

    override suspend fun saveResolutionResponse(response: VendorPortalResolutionResponse): DomainResult<VendorPortalResolutionResponse> {
        synchronized(lock) {
            val key = "${response.tenantId}:${response.projectId}:${response.vendorId}:${response.disputeId}"
            val list = resolutionResponses.getOrPut(key) { mutableListOf() }
            list.add(response)
            return DomainResult.Success(response)
        }
    }

    override suspend fun listResolutionResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<List<VendorPortalResolutionResponse>> {
        synchronized(lock) {
            val key = "$tenantId:$projectId:$vendorId:$disputeId"
            val list = resolutionResponses[key] ?: emptyList()
            return DomainResult.Success(list.sortedByDescending { it.respondedAt })
        }
    }

    override suspend fun saveEvidence(evidence: VendorPortalQualityEvidence): DomainResult<VendorPortalQualityEvidence> {
        synchronized(lock) {
            val key = "${evidence.tenantId}:${evidence.projectId}:${evidence.vendorId}:${evidence.evidenceId}"
            evidenceMap[key] = evidence
            return DomainResult.Success(evidence)
        }
    }

    override suspend fun findEvidenceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evidenceId: String
    ): DomainResult<VendorPortalQualityEvidence?> {
        synchronized(lock) {
            val key = "$tenantId:$projectId:$vendorId:$evidenceId"
            return DomainResult.Success(evidenceMap[key])
        }
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalQualityEvidence>> {
        synchronized(lock) {
            val res = evidenceMap.values
                .filter { it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId }
                .filter { it.entityType == entityType && it.entityId == entityId }
                .sortedByDescending { it.uploadedAt }
            return DomainResult.Success(res)
        }
    }

    override suspend fun recordAudit(activity: VendorPortalQualityActivity): DomainResult<Unit> {
        synchronized(lock) {
            val key = "${activity.tenantId}:${activity.projectId}:${activity.vendorId}:${activity.entityType}:${activity.entityId}"
            val list = auditEvents.getOrPut(key) { mutableListOf() }
            list.add(activity)
            return DomainResult.Success(Unit)
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalQualityActivity>> {
        synchronized(lock) {
            val key = "$tenantId:$projectId:$vendorId:$entityType:$entityId"
            val list = auditEvents[key] ?: emptyList()
            return DomainResult.Success(list.sortedByDescending { it.timestamp })
        }
    }
}
