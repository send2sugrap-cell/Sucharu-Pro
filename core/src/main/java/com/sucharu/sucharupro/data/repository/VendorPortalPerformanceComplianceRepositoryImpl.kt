package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPortalPerformanceComplianceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalPerformanceComplianceRepository

/**
 * Implementation of VendorPortalPerformanceComplianceRepository.
 */
class VendorPortalPerformanceComplianceRepositoryImpl(
    private val dataSource: VendorPortalPerformanceComplianceDataSource
) : VendorPortalPerformanceComplianceRepository {

    override suspend fun saveEvaluationResponse(response: VendorPortalEvaluationResponse): DomainResult<VendorPortalEvaluationResponse> =
        try {
            DomainResult.Success(dataSource.saveEvaluationResponse(response))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save evaluation response")
        }

    override suspend fun findEvaluationResponseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        responseId: String
    ): DomainResult<VendorPortalEvaluationResponse?> =
        try {
            DomainResult.Success(dataSource.findEvaluationResponseById(tenantId, projectId, vendorId, responseId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find evaluation response")
        }

    override suspend fun listEvaluationResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evaluationId: String
    ): DomainResult<List<VendorPortalEvaluationResponse>> =
        try {
            DomainResult.Success(dataSource.listEvaluationResponses(tenantId, projectId, vendorId, evaluationId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list evaluation responses")
        }

    override suspend fun saveComplianceEvidence(evidence: VendorPortalComplianceEvidence): DomainResult<VendorPortalComplianceEvidence> =
        try {
            DomainResult.Success(dataSource.saveComplianceEvidence(evidence))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save compliance evidence")
        }

    override suspend fun findComplianceEvidenceById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evidenceId: String
    ): DomainResult<VendorPortalComplianceEvidence?> =
        try {
            DomainResult.Success(dataSource.findComplianceEvidenceById(tenantId, projectId, vendorId, evidenceId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find compliance evidence")
        }

    override suspend fun listComplianceEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        recordId: String?,
        actionId: String?
    ): DomainResult<List<VendorPortalComplianceEvidence>> =
        try {
            DomainResult.Success(dataSource.listComplianceEvidence(tenantId, projectId, vendorId, recordId, actionId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list compliance evidence")
        }

    override suspend fun saveCorrectiveActionResponse(response: VendorPortalCorrectiveActionResponse): DomainResult<VendorPortalCorrectiveActionResponse> =
        try {
            DomainResult.Success(dataSource.saveCorrectiveActionResponse(response))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to save corrective action response")
        }

    override suspend fun findCorrectiveActionResponseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        responseId: String
    ): DomainResult<VendorPortalCorrectiveActionResponse?> =
        try {
            DomainResult.Success(dataSource.findCorrectiveActionResponseById(tenantId, projectId, vendorId, responseId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to find corrective action response")
        }

    override suspend fun listCorrectiveActionResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String
    ): DomainResult<List<VendorPortalCorrectiveActionResponse>> =
        try {
            DomainResult.Success(dataSource.listCorrectiveActionResponses(tenantId, projectId, vendorId, actionId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list corrective action responses")
        }

    override suspend fun recordAudit(activity: VendorPortalPerformanceComplianceActivity): DomainResult<Unit> =
        try {
            dataSource.recordAudit(activity)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to record audit event")
        }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): DomainResult<List<VendorPortalPerformanceComplianceActivity>> =
        try {
            DomainResult.Success(dataSource.listAuditEvents(tenantId, projectId, vendorId, entityType, entityId))
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = e.message ?: "Failed to list audit events")
        }
}
