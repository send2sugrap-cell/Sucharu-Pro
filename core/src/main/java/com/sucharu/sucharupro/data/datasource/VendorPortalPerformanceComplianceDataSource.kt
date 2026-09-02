package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * DataSource contract for Vendor Portal Performance & Compliance Persistence.
 */
interface VendorPortalPerformanceComplianceDataSource {

    // Evaluation Responses
    suspend fun saveEvaluationResponse(response: VendorPortalEvaluationResponse): VendorPortalEvaluationResponse
    suspend fun findEvaluationResponseById(tenantId: String, projectId: String, vendorId: String, responseId: String): VendorPortalEvaluationResponse?
    suspend fun listEvaluationResponses(tenantId: String, projectId: String, vendorId: String, evaluationId: String): List<VendorPortalEvaluationResponse>

    // Compliance Evidence
    suspend fun saveComplianceEvidence(evidence: VendorPortalComplianceEvidence): VendorPortalComplianceEvidence
    suspend fun findComplianceEvidenceById(tenantId: String, projectId: String, vendorId: String, evidenceId: String): VendorPortalComplianceEvidence?
    suspend fun listComplianceEvidence(tenantId: String, projectId: String, vendorId: String, recordId: String? = null, actionId: String? = null): List<VendorPortalComplianceEvidence>

    // Corrective Action Responses
    suspend fun saveCorrectiveActionResponse(response: VendorPortalCorrectiveActionResponse): VendorPortalCorrectiveActionResponse
    suspend fun findCorrectiveActionResponseById(tenantId: String, projectId: String, vendorId: String, responseId: String): VendorPortalCorrectiveActionResponse?
    suspend fun listCorrectiveActionResponses(tenantId: String, projectId: String, vendorId: String, actionId: String): List<VendorPortalCorrectiveActionResponse>

    // Audit Events
    suspend fun recordAudit(activity: VendorPortalPerformanceComplianceActivity)
    suspend fun listAuditEvents(tenantId: String, projectId: String, vendorId: String, entityType: String? = null, entityId: String? = null): List<VendorPortalPerformanceComplianceActivity>
}
