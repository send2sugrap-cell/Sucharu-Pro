package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe fake datasource for unit and fast integration tests.
 */
class FakeVendorPortalPerformanceComplianceDataSource : VendorPortalPerformanceComplianceDataSource {

    private val evaluationResponses = ConcurrentHashMap<String, VendorPortalEvaluationResponse>()
    private val complianceEvidenceMap = ConcurrentHashMap<String, VendorPortalComplianceEvidence>()
    private val correctiveActionResponses = ConcurrentHashMap<String, VendorPortalCorrectiveActionResponse>()
    private val auditEvents = ConcurrentHashMap<String, VendorPortalPerformanceComplianceActivity>()

    override suspend fun saveEvaluationResponse(response: VendorPortalEvaluationResponse): VendorPortalEvaluationResponse {
        val key = "${response.tenantId}_${response.responseId}"
        evaluationResponses[key] = response
        return response
    }

    override suspend fun findEvaluationResponseById(tenantId: String, projectId: String, vendorId: String, responseId: String): VendorPortalEvaluationResponse? {
        val key = "${tenantId}_${responseId}"
        val record = evaluationResponses[key] ?: return null
        if (record.projectId != projectId || record.vendorId != vendorId) return null
        return record
    }

    override suspend fun listEvaluationResponses(tenantId: String, projectId: String, vendorId: String, evaluationId: String): List<VendorPortalEvaluationResponse> {
        return evaluationResponses.values.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId && it.evaluationId == evaluationId
        }.sortedByDescending { it.submittedAt }
    }

    override suspend fun saveComplianceEvidence(evidence: VendorPortalComplianceEvidence): VendorPortalComplianceEvidence {
        val key = "${evidence.tenantId}_${evidence.evidenceId}"
        complianceEvidenceMap[key] = evidence
        return evidence
    }

    override suspend fun findComplianceEvidenceById(tenantId: String, projectId: String, vendorId: String, evidenceId: String): VendorPortalComplianceEvidence? {
        val key = "${tenantId}_${evidenceId}"
        val record = complianceEvidenceMap[key] ?: return null
        if (record.projectId != projectId || record.vendorId != vendorId) return null
        return record
    }

    override suspend fun listComplianceEvidence(tenantId: String, projectId: String, vendorId: String, recordId: String?, actionId: String?): List<VendorPortalComplianceEvidence> {
        return complianceEvidenceMap.values.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId &&
                    (recordId == null || it.recordId == recordId) &&
                    (actionId == null || it.actionId == actionId)
        }.sortedByDescending { it.uploadedAt }
    }

    override suspend fun saveCorrectiveActionResponse(response: VendorPortalCorrectiveActionResponse): VendorPortalCorrectiveActionResponse {
        val key = "${response.tenantId}_${response.responseId}"
        correctiveActionResponses[key] = response
        return response
    }

    override suspend fun findCorrectiveActionResponseById(tenantId: String, projectId: String, vendorId: String, responseId: String): VendorPortalCorrectiveActionResponse? {
        val key = "${tenantId}_${responseId}"
        val record = correctiveActionResponses[key] ?: return null
        if (record.projectId != projectId || record.vendorId != vendorId) return null
        return record
    }

    override suspend fun listCorrectiveActionResponses(tenantId: String, projectId: String, vendorId: String, actionId: String): List<VendorPortalCorrectiveActionResponse> {
        return correctiveActionResponses.values.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId && it.actionId == actionId
        }.sortedByDescending { it.submittedAt }
    }

    override suspend fun recordAudit(activity: VendorPortalPerformanceComplianceActivity) {
        val key = "${activity.tenantId}_${activity.activityId}"
        auditEvents[key] = activity
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, vendorId: String, entityType: String?, entityId: String?): List<VendorPortalPerformanceComplianceActivity> {
        return auditEvents.values.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.vendorId == vendorId &&
                    (entityType == null || it.entityType == entityType) &&
                    (entityId == null || it.entityId == entityId)
        }.sortedByDescending { it.occurredAt }
    }
}
