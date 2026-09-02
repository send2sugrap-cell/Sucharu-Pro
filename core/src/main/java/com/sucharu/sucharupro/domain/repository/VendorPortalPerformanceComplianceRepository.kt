package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Repository interface for Vendor Portal Performance & Compliance Workspace (Module 13 Step 08).
 */
interface VendorPortalPerformanceComplianceRepository {

    // Evaluation Responses
    suspend fun saveEvaluationResponse(response: VendorPortalEvaluationResponse): DomainResult<VendorPortalEvaluationResponse>
    suspend fun findEvaluationResponseById(tenantId: String, projectId: String, vendorId: String, responseId: String): DomainResult<VendorPortalEvaluationResponse?>
    suspend fun listEvaluationResponses(tenantId: String, projectId: String, vendorId: String, evaluationId: String): DomainResult<List<VendorPortalEvaluationResponse>>

    // Compliance Evidence
    suspend fun saveComplianceEvidence(evidence: VendorPortalComplianceEvidence): DomainResult<VendorPortalComplianceEvidence>
    suspend fun findComplianceEvidenceById(tenantId: String, projectId: String, vendorId: String, evidenceId: String): DomainResult<VendorPortalComplianceEvidence?>
    suspend fun listComplianceEvidence(tenantId: String, projectId: String, vendorId: String, recordId: String? = null, actionId: String? = null): DomainResult<List<VendorPortalComplianceEvidence>>

    // Corrective Action Responses
    suspend fun saveCorrectiveActionResponse(response: VendorPortalCorrectiveActionResponse): DomainResult<VendorPortalCorrectiveActionResponse>
    suspend fun findCorrectiveActionResponseById(tenantId: String, projectId: String, vendorId: String, responseId: String): DomainResult<VendorPortalCorrectiveActionResponse?>
    suspend fun listCorrectiveActionResponses(tenantId: String, projectId: String, vendorId: String, actionId: String): DomainResult<List<VendorPortalCorrectiveActionResponse>>

    // Audit Events
    suspend fun recordAudit(activity: VendorPortalPerformanceComplianceActivity): DomainResult<Unit>
    suspend fun listAuditEvents(tenantId: String, projectId: String, vendorId: String, entityType: String? = null, entityId: String? = null): DomainResult<List<VendorPortalPerformanceComplianceActivity>>
}
